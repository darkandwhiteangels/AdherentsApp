package com.antechrist.adherentsapp.domain.usecase

import android.util.Log
import com.antechrist.adherentsapp.domain.repository.GuardiansRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

private const val TAG = "IsGuardianLinked"

class IsGuardianLinkedToCurrentUserUseCase @Inject constructor(
    private val guardiansRepo: GuardiansRepository
) {
    suspend operator fun invoke(): Boolean {
        Log.d(TAG, "════════════════════════════════════════════")
        Log.d(TAG, "🔗 Vérification liaison Guardian ↔ User")

        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        Log.d(TAG, "✅ Authenticated: ${currentUser != null}")
        Log.d(TAG, "🆔 Auth UID: $uid")
        Log.d(TAG, "📧 Auth Email: ${currentUser?.email}")

        if (uid == null) {
            Log.w(TAG, "⚠️ UID null, utilisateur non connecté")
            Log.d(TAG, "════════════════════════════════════════════")
            return false
        }

        return try {
            val guardian = guardiansRepo.getByAuthUid(uid)
            val isLinked = guardian != null

            if (isLinked) {
                Log.d(TAG, "📊 Result: ✅ Guardian lié trouvé")
                Log.d(TAG, "   - Guardian ID: ${guardian?.id}")
                Log.d(TAG, "   - Guardian Nom: ${guardian?.nom} ${guardian?.prenom}")
                Log.d(TAG, "   - Guardian Email: ${guardian?.email}")
            } else {
                Log.d(TAG, "📊 Result: ⚠️ Aucun guardian lié à cet UID")
            }

            Log.d(TAG, "════════════════════════════════════════════")
            isLinked

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERREUR lors de la vérification", e)
            Log.e(TAG, "Type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            Log.d(TAG, "════════════════════════════════════════════")
            false
        }
    }
}