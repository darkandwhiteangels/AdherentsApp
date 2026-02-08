package com.antechrist.adherentsapp.ui.screens.parent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "ParentActivation"

enum class ActivationState {
    LOADING,        // Vérification en cours
    NEED_LOGIN,     // Utilisateur doit se connecter
    EMAIL_MISMATCH, // Email ne correspond pas
    EXPIRED,        // Invitation expirée
    INVALID,        // Token invalide ou déjà utilisé
    SUCCESS,        // Activation réussie
    ERROR           // Erreur technique
}

data class ParentActivationUi(
    val state: ActivationState = ActivationState.LOADING,
    val error: String? = null,
    val guardianName: String? = null,
    val email: String? = null,
    val currentUserEmail: String? = null
)

@HiltViewModel
class ParentActivationViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _ui = MutableStateFlow(ParentActivationUi())
    val ui: StateFlow<ParentActivationUi> = _ui.asStateFlow()

    /**
     * Démarre le processus d'activation avec le token
     */
    fun activate(token: String) {
        viewModelScope.launch {
            _ui.update { it.copy(state = ActivationState.LOADING, error = null) }

            try {
                Log.d(TAG, "========================================")
                Log.d(TAG, "=== DÉBUT ACTIVATION PARENT ===")
                Log.d(TAG, "Token: $token")

                // 1. Vérifier que l'utilisateur est connecté
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Log.w(TAG, "⚠️ Utilisateur non connecté")
                    _ui.update {
                        it.copy(
                            state = ActivationState.NEED_LOGIN,
                            error = null
                        )
                    }
                    Log.d(TAG, "=== FIN ACTIVATION PARENT ===")
                    Log.d(TAG, "========================================")
                    return@launch
                }

                val currentUserEmail = currentUser.email?.lowercase()
                Log.d(TAG, "Utilisateur connecté: $currentUserEmail (uid: ${currentUser.uid})")

                // 2. Rechercher le household avec ce token via collectionGroup
                Log.d(TAG, "Recherche du household avec token...")
                val householdsQuery = db.collectionGroup("households")
                    .whereEqualTo("invitation.token", token)
                    .limit(1)
                    .get()
                    .await()

                if (householdsQuery.isEmpty) {
                    Log.w(TAG, "⚠️ Aucun household trouvé avec ce token")
                    _ui.update {
                        it.copy(
                            state = ActivationState.INVALID,
                            error = "Cette invitation est introuvable ou a déjà été utilisée"
                        )
                    }
                    Log.d(TAG, "=== FIN ACTIVATION PARENT ===")
                    Log.d(TAG, "========================================")
                    return@launch
                }

                val householdDoc = householdsQuery.documents.first()
                val householdData = householdDoc.data ?: throw Exception("Household data null")

                Log.d(TAG, "✅ Household trouvé: ${householdDoc.id}")
                Log.d(TAG, "Path: ${householdDoc.reference.path}")

                // 3. Extraire les données d'invitation
                @Suppress("UNCHECKED_CAST")
                val invitation = householdData["invitation"] as? Map<String, Any>
                    ?: throw Exception("Invitation data null")

                val invitationStatus = invitation["status"] as? String
                val invitationEmail = (invitation["email"] as? String)?.lowercase()
                val expiresAt = invitation["expiresAt"] as? Long ?: 0L
                val guardianId = householdData["guardianId"] as? String
                    ?: throw Exception("guardianId null")

                Log.d(TAG, "Invitation:")
                Log.d(TAG, "  - Status: $invitationStatus")
                Log.d(TAG, "  - Email: $invitationEmail")
                Log.d(TAG, "  - ExpiresAt: $expiresAt")
                Log.d(TAG, "  - GuardianId: $guardianId")

                // 4. Vérifier que l'invitation est PENDING
                if (invitationStatus != "PENDING") {
                    Log.w(TAG, "⚠️ Invitation déjà utilisée (status: $invitationStatus)")
                    _ui.update {
                        it.copy(
                            state = ActivationState.INVALID,
                            error = "Cette invitation a déjà été utilisée"
                        )
                    }
                    Log.d(TAG, "=== FIN ACTIVATION PARENT ===")
                    Log.d(TAG, "========================================")
                    return@launch
                }

                // 5. Vérifier l'expiration
                val now = System.currentTimeMillis()
                if (now > expiresAt) {
                    Log.w(TAG, "⚠️ Invitation expirée")
                    val daysAgo = (now - expiresAt) / (1000 * 60 * 60 * 24)
                    _ui.update {
                        it.copy(
                            state = ActivationState.EXPIRED,
                            error = "Cette invitation a expiré il y a $daysAgo jour(s)"
                        )
                    }
                    Log.d(TAG, "=== FIN ACTIVATION PARENT ===")
                    Log.d(TAG, "========================================")
                    return@launch
                }

                // 6. Récupérer les infos du guardian pour affichage
                val guardianDoc = db.collection("guardians").document(guardianId).get().await()
                val guardianName = if (guardianDoc.exists()) {
                    val nom = guardianDoc.getString("nom") ?: ""
                    val prenom = guardianDoc.getString("prenom") ?: ""
                    "$prenom $nom".trim()
                } else {
                    "Parent"
                }

                Log.d(TAG, "Guardian: $guardianName")

                // 7. Vérifier que l'email correspond
                if (currentUserEmail != invitationEmail) {
                    Log.w(TAG, "⚠️ Email ne correspond pas")
                    Log.w(TAG, "  Attendu: $invitationEmail")
                    Log.w(TAG, "  Reçu: $currentUserEmail")
                    _ui.update {
                        it.copy(
                            state = ActivationState.EMAIL_MISMATCH,
                            error = "Votre email ne correspond pas à l'invitation",
                            email = invitationEmail,
                            currentUserEmail = currentUserEmail,
                            guardianName = guardianName
                        )
                    }
                    Log.d(TAG, "=== FIN ACTIVATION PARENT ===")
                    Log.d(TAG, "========================================")
                    return@launch
                }

                Log.d(TAG, "✅ Email correspond")

                // 8. Accepter l'invitation (batch write)
                Log.d(TAG, "Acceptation de l'invitation...")

                val batch = db.batch()

                // a) Mettre à jour household.invitation
                batch.update(
                    householdDoc.reference,
                    mapOf(
                        "invitation.status" to "ACCEPTED",
                        "invitation.acceptedAt" to now,
                        "invitation.acceptedByUid" to currentUser.uid,
                        "updatedAt" to now
                    )
                )

                // b) Mettre à jour guardian.authUid
                batch.update(
                    db.collection("guardians").document(guardianId),
                    mapOf(
                        "authUid" to currentUser.uid,
                        "updatedAt" to now
                    )
                )

                batch.commit().await()

                Log.d(TAG, "✅ Invitation acceptée avec succès !")
                Log.d(TAG, "  - household.invitation.status = ACCEPTED")
                Log.d(TAG, "  - guardian.authUid = ${currentUser.uid}")

                // 9. Succès
                _ui.update {
                    it.copy(
                        state = ActivationState.SUCCESS,
                        error = null,
                        guardianName = guardianName,
                        email = invitationEmail
                    )
                }

                Log.d(TAG, "=== FIN ACTIVATION PARENT ===")
                Log.d(TAG, "========================================")

            } catch (e: Exception) {
                Log.e(TAG, "❌ ERREUR lors de l'activation", e)
                Log.e(TAG, "Type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Message: ${e.message}")

                _ui.update {
                    it.copy(
                        state = ActivationState.ERROR,
                        error = e.message ?: "Erreur inconnue lors de l'activation"
                    )
                }

                Log.d(TAG, "========================================")
            }
        }
    }

    /**
     * Réinitialise l'état (utile pour retry)
     */
    fun reset() {
        _ui.update { ParentActivationUi() }
    }
}