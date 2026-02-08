package com.antechrist.adherentsapp

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import com.antechrist.adherentsapp.ui.notifications.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    private lateinit var auth: FirebaseAuth
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createChannel(this)
        }

        // 🔊 Firestore verbose si l’app est debuggable (pas besoin de BuildConfig)
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) {
            FirebaseFirestore.setLoggingEnabled(true)
        }

        // 👤 Logs claims + éventuel doc user_roles/{uid}
        auth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener { fa ->
            val user = fa.currentUser
            if (user == null) {
                Log.d(TAG, "Auth: signed OUT")
            } else {
                Log.d(TAG, "Auth: signed IN uid=${user.uid}")
                user.getIdToken(true)
                    .addOnSuccessListener { res ->
                        Log.d(TAG, "Auth claims: ${res.claims}") // ex: {role=super_admin, …}
                        logRoleDoc(user.uid)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "getIdToken failed", e)
                    }
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun logRoleDoc(uid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("user_roles").document(uid).get()
            .addOnSuccessListener { snap ->
                Log.d(TAG, "roleDoc($uid) exists=${snap.exists()} data=${snap.data}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "roleDoc read failed", e)
            }
    }

    companion object {
        private const val TAG = "AppDebug"
    }
}
