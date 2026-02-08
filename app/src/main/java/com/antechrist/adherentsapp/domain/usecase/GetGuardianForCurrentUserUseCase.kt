package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.domain.repository.GuardiansRepository
import com.antechrist.adherentsapp.core.debug.FireLog
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Récupère le Guardian associé au user actuellement connecté.
 *
 * Processus :
 * 1. Récupère l'UID Firebase Auth du user connecté
 * 2. Lit le doc users/{uid} pour obtenir le guardianId
 * 3. Récupère le Guardian via guardianId
 */
class GetGuardianForCurrentUserUseCase @Inject constructor(
    private val guardiansRepository: GuardiansRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(): Guardian? {
        val TAG = "LoginTrace"

        val user = auth.currentUser
        if (user == null) {
            FireLog.step(TAG, "Auth: currentUser = null")
            return null
        }

        FireLog.step(TAG, "Auth: uid=${user.uid} email=${user.email}")

        // ✅ force refresh token (claims)
        try {
            val token = user.getIdToken(true).await()
            val claims = token.claims
            FireLog.step(TAG, "Token claims: role=${claims["role"]} parent=${claims["parent"]} roles=${claims["roles"]}")
        } catch (e: Exception) {
            FireLog.err(TAG, "Token refresh FAILED", e)
        }

        // users/{uid}
        try {
            FireLog.step(TAG, "Firestore GET users/${user.uid} START")
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            FireLog.step(TAG, "Firestore GET users/${user.uid} OK exists=${userDoc.exists()}")

            if (!userDoc.exists()) return null

            val role = userDoc.getString("role")
            val guardianId = userDoc.getString("guardianId")
            FireLog.step(TAG, "users/${user.uid}: role=$role guardianId=$guardianId")

            if (guardianId.isNullOrBlank()) return null

            FireLog.step(TAG, "Repo guardians.getById($guardianId) START")
            val g = guardiansRepository.getById(guardianId)
            FireLog.step(TAG, "Repo guardians.getById($guardianId) OK guardian=${g?.nom} ${g?.prenom}")
            return g
        } catch (e: Exception) {
            FireLog.err(TAG, "Firestore users/{uid} or guardian load FAILED", e)
            return null
        }
    }
}