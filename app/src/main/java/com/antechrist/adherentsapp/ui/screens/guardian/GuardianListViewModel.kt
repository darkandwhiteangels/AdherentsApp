package com.antechrist.adherentsapp.ui.screens.guardian

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.functions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

data class GuardianUi(
    val id: String,
    val nom: String,
    val prenom: String,
    val email: String?,
    val telephone: String?,
    val ville: String?,
    val relation: String?,
    val authUid: String?,
    val memberNames: List<String> = emptyList(), // Prénoms des enfants du foyer
    val memberIds: List<String> = emptyList()    // 🆕 IDs des adhérents rattachés
)

data class GuardianListState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<GuardianUi> = emptyList(),
    val query: String = "",
    // SÉLECTION MULTIPLE
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    // INVITATION
    val sendingInvitations: Boolean = false,
    val invitationSuccess: String? = null
) {
    val filtered: List<GuardianUi>
        get() {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return items
            return items.filter { g ->
                g.nom.lowercase().contains(q) ||
                        g.prenom.lowercase().contains(q) ||
                        (g.email?.lowercase()?.contains(q) == true) ||
                        (g.telephone?.contains(q) == true) ||
                        (g.ville?.lowercase()?.contains(q) == true) ||
                        g.memberNames.any { it.lowercase().contains(q) }  // 🆕 Recherche dans les prénoms enfants
            }
        }

    // Helpers
    val hasSelection: Boolean get() = selectedIds.isNotEmpty()
    val selectedCount: Int get() = selectedIds.size
}

@HiltViewModel
class GuardianListViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _ui = MutableStateFlow(GuardianListState(loading = true))
    val ui: StateFlow<GuardianListState> = _ui

    init {
        viewModelScope.launch { load() }
    }

    fun onChangeQuery(v: String) {
        _ui.update { it.copy(query = v) }
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    // SÉLECTION MULTIPLE

    fun toggleSelection(guardianId: String) {
        _ui.update { state ->
            val newSelection = if (guardianId in state.selectedIds) {
                state.selectedIds - guardianId
            } else {
                state.selectedIds + guardianId
            }
            state.copy(
                selectedIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _ui.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun selectAll() {
        _ui.update { state ->
            state.copy(
                selectedIds = state.filtered.map { it.id }.toSet(),
                isSelectionMode = true
            )
        }
    }

    // SYSTÈME D'INVITATION
    fun sendInvitations(selectedIds: Set<String>) {
        viewModelScope.launch {
            if (selectedIds.isEmpty()) {
                Log.w("GuardianInvite", "⚠️ AUCUN GUARDIAN SÉLECTIONNÉ")
                return@launch
            }

            _ui.update { it.copy(sendingInvitations = true, error = null, invitationSuccess = null) }

            try {
                val functions = Firebase.functions("europe-west1")

                var successCount = 0
                var errorCount = 0
                val errors = mutableListOf<String>()

                for (guardianId in selectedIds) {
                    try {
                        val guardian = _ui.value.items.find { it.id == guardianId }
                            ?: throw Exception("Guardian introuvable")

                        val email = guardian.email?.trim()?.lowercase()
                            ?.takeIf { it.isNotBlank() && it.contains("@") }
                            ?: throw Exception("${guardian.nom}: Email invalide")

                        if (!guardian.authUid.isNullOrBlank()) {
                            throw Exception("${guardian.nom}: Compte déjà activé")
                        }
                        // ✅ APPELER LA CLOUD FUNCTION EXISTANTE
                        val data = hashMapOf(
                            "email" to email,
                            "role" to "parent",
                            "guardianId" to guardianId
                        )

                        val result = functions
                            .getHttpsCallable("setUserRoleByEmail")
                            .call(data)
                            .await()

                        val response = result.data as? Map<*, *>

                        if (response?.get("ok") == true) {
                            val uid = response["uid"] as? String
                            val created = response["created"] as? Boolean ?: false

                            // La Cloud Function a déjà :
                            // 1. Créé le compte Auth (ou utilisé l'existant)
                            // 2. Défini les custom claims (role: parent, parent: true)
                            // 3. Créé le doc users/{uid}
                            // 4. Mis à jour guardian.authUid

                            // ═══════════════════════════════════════════════════════
                            // 🆕 2. ENVOYER EMAIL DE RÉINITIALISATION (DEPUIS CLIENT)
                            // ═══════════════════════════════════════════════════════

                            try {
                                // ✅ UTILISER Firebase Auth DIRECTEMENT (pas Cloud Function)
                                Firebase.auth.sendPasswordResetEmail(email).await()

                            } catch (emailError: Exception) {
                                // Ne pas bloquer si l'email échoue
                            }
                            // ═══════════════════════════════════════════════════════

                            // ═══════════════════════════════════════════════════════
                            // 🆕 3. CRÉER LE HOUSEHOLD POUR LA SAISON ACTUELLE
                            // ═══════════════════════════════════════════════════════

                            try {
                                val seasonKey = getCurrentSeasonKey()
                                val householdRef = db.collection("cotisations_families")
                                    .document(seasonKey)
                                    .collection("households")
                                    .document(guardianId)

                                // Vérifier si le household existe déjà
                                val existingHousehold = householdRef.get().await()

                                if (!existingHousehold.exists()) {
                                    // ✅ CRÉER HOUSEHOLD AVEC TOUS LES CHAMPS REQUIS
                                    val householdData = hashMapOf(
                                        // REQUIS - IDs
                                        "guardianId" to guardianId,
                                        "seasonKey" to seasonKey,
                                        "memberIds" to guardian.memberIds.distinct(),

                                        // REQUIS - Booleans
                                        "oneClassPerWeek" to false,
                                        "exemptFromFee" to false,
                                        "discountBlackBeltEnabled" to false,
                                        "discountFamilyGradedEnabled" to false,
                                        "discountAssistantProfEnabled" to false,

                                        // REQUIS - Status
                                        "status" to "A_REGLER",

                                        // REQUIS - Montants (int >= 0)
                                        "amountBaseCents" to 0,
                                        "discountCents" to 0,
                                        "manualDiscountCents" to 0,
                                        "amountDueCents" to 0,
                                        "licenceCount" to 0,
                                        "licenceAmountCents" to 0,
                                        "licencesTotalCents" to 0,

                                        // REQUIS - Listes
                                        "paymentsPlan" to listOf<Map<String, Any>>(),
                                        "paymentsReceived" to listOf<Map<String, Any>>(),
                                        "aids" to listOf<Map<String, Any>>(),

                                        // OPTIONNELS
                                        "updatedAt" to System.currentTimeMillis(),
                                        "updatedBy" to (Firebase.auth.currentUser?.uid ?: "system")
                                    )

                                    householdRef.set(householdData).await()

                                } else {
                                    // ✅ Mise à jour memberIds si nécessaire (sans casser le reste)
                                    val newMemberIds = guardian.memberIds.distinct()
                                    householdRef.update(
                                        mapOf(
                                            "memberIds" to newMemberIds,
                                            "updatedAt" to System.currentTimeMillis(),
                                            "updatedBy" to (Firebase.auth.currentUser?.uid ?: "system")
                                        )
                                    ).await()

                                }
                            } catch (householdError: Exception) {
                                // Ne pas bloquer si le household échoue (mais on log l'erreur)
                            }
                            // ═══════════════════════════════════════════════════════

                            successCount++
                        } else {
                            throw Exception("Cloud Function a échoué: ${response?.get("message")}")
                        }

                    } catch (e: Exception) {
                        errorCount++
                        val guardian = _ui.value.items.find { it.id == guardianId }
                        val errorMsg = "${guardian?.nom ?: "?"}: ${e.message}"
                        errors.add(errorMsg)
                    }
                }

                if (errors.isNotEmpty()) {
                    Log.e("GuardianInvite", "📝 Erreurs: ${errors.joinToString(", ")}")
                }

                val message = when {
                    errorCount == 0 -> "✅ $successCount invitation(s) envoyée(s)"
                    successCount == 0 -> "❌ Échec de toutes les invitations"
                    else -> "⚠️ $successCount réussie(s), $errorCount échec(s)"
                }

                _ui.update {
                    it.copy(
                        sendingInvitations = false,
                        invitationSuccess = message,
                        selectedIds = emptySet(),
                        isSelectionMode = false,
                        error = if (errors.isNotEmpty()) errors.joinToString("\n") else null
                    )
                }

            } catch (e: Exception) {

                _ui.update {
                    it.copy(
                        sendingInvitations = false,
                        error = e.message ?: "Erreur lors de l'envoi des invitations"
                    )
                }
            }
        }
    }


    private fun getCurrentSeasonKey(): String {
        val now = LocalDate.now()
        val year = now.year
        val month = now.monthValue

        return if (month >= 9) {
            "$year-${year + 1}"
        } else {
            "${year - 1}-$year"
        }
    }

    // CHARGEMENT DES GUARDIANS AVEC MEMBRES DU FOYER

    private suspend fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        try {
            // 1. Récupérer tous les guardians
            val guardiansSnap = db.collection("guardians")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(500)
                .get().await()

            // 2. Récupérer tous les adhérents pour mapper guardianId -> prénoms
            val adherentsSnap = db.collection("adherents")
                .get().await()

            // Maps guardianId -> prénoms / ids
            val guardianToMemberNames = mutableMapOf<String, MutableList<String>>()
            val guardianToMemberIds = mutableMapOf<String, MutableList<String>>()


            for (adherentDoc in adherentsSnap.documents) {
                val adherentId = adherentDoc.id
                val prenom = adherentDoc.getString("prenom") ?: ""

                fun attach(guardianId: String) {
                    if (guardianId.isBlank()) return
                    if (prenom.isNotBlank()) {
                        guardianToMemberNames.getOrPut(guardianId) { mutableListOf() }.add(prenom)
                    }
                    guardianToMemberIds.getOrPut(guardianId) { mutableListOf() }.add(adherentId)
                }

                // primaryGuardianId
                val primaryId = adherentDoc.getString("primaryGuardianId")
                if (!primaryId.isNullOrBlank()) attach(primaryId)

                // guardianIds array
                @Suppress("UNCHECKED_CAST")
                val guardianIds = adherentDoc.get("guardianIds") as? List<String>
                guardianIds?.forEach { gid ->
                    if (gid.isNotBlank()) attach(gid)
                }
            }


            // 3. Construire la liste des GuardianUi avec memberNames
            val list = guardiansSnap.documents.map { d ->
                val guardianId = d.id
                val memberNames = guardianToMemberNames[guardianId]?.distinct() ?: emptyList()
                val memberIds = guardianToMemberIds[guardianId]?.distinct() ?: emptyList()

                GuardianUi(
                    id = guardianId,
                    nom = (d.getString("nom") ?: "").trim(),
                    prenom = (d.getString("prenom") ?: "").trim(),
                    email = d.getString("email"),
                    telephone = d.getString("telephone"),
                    ville = d.getString("ville"),
                    relation = d.getString("relation"),
                    authUid = d.getString("authUid"),
                    memberNames = memberNames,
                    memberIds = memberIds
                )
            }.sortedWith(
                compareBy<GuardianUi> { it.nom }.thenBy { it.prenom }
            )

            _ui.update { it.copy(loading = false, items = list, error = null) }
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = e.message ?: "Erreur de chargement") }
        }
    }

    suspend fun deleteGuardianSafely(guardianId: String): Boolean {
        _ui.update { it.copy(loading = true, error = null) }
        return try {
            val primarySnap = db.collection("adherents")
                .whereEqualTo("primaryGuardianId", guardianId)
                .limit(1)
                .get()
                .await()

            if (!primarySnap.isEmpty) {
                _ui.update { it.copy(loading = false) }
                return false
            }

            val listSnap = db.collection("adherents")
                .whereArrayContains("guardianIds", guardianId)
                .limit(1)
                .get()
                .await()

            if (!listSnap.isEmpty) {
                _ui.update { it.copy(loading = false) }
                return false
            }

            db.collection("guardians").document(guardianId).delete().await()

            _ui.update { state ->
                state.copy(
                    loading = false,
                    items = state.items.filterNot { it.id == guardianId },
                    error = null
                )
            }
            true
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = e.message ?: "Échec de la suppression") }
            false
        }
    }

}