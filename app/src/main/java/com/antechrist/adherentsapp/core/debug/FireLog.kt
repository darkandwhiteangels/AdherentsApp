package com.antechrist.adherentsapp.core.debug

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

object FireLog {
    fun step(tag: String, msg: String) {
        Log.d(tag, "🧩 $msg")
    }

    fun err(tag: String, msg: String, e: Throwable) {
        val code = (e as? FirebaseFirestoreException)?.code?.name
        Log.e(tag, "🔥 $msg${if (code != null) " | code=$code" else ""} | ${e.message}", e)
    }
}
