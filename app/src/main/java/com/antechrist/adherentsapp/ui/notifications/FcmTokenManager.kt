package com.antechrist.adherentsapp.ui.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Stocke le token FCM dans Firestore sous users/{uid}/fcmTokens/{token}
 * (optionnel mais propre si tu veux envoyer du ciblé par utilisateur)
 */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    fun saveTokenLocallyAndRemote(token: String) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.w(
                TAG,
                "saveTokenLocallyAndRemote(): aucun utilisateur connecté, " +
                        "on ne peut pas enregistrer le token = $token pour le moment"
            )
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val data = mapOf(
            "token" to token,
            "platform" to "android",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .collection("fcmTokens")
            .document(token)
            .set(data)
            .addOnSuccessListener { Log.d(TAG, "Token FCM enregistré pour uid=$uid : $token") }
            .addOnFailureListener { Log.e(TAG, "Token save failed pour uid=$uid", it) }
    }
}
