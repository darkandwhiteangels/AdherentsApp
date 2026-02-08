package com.antechrist.adherentsapp.ui.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object TopicSubscription {

    private const val TOPIC_ADULT_PARENT = "adult_parent"
    private const val TAG = "TopicSubscription"

    fun updateAdultParentSubscription(isAdultOrResponsible: Boolean) {
        val fm = FirebaseMessaging.getInstance()
        if (isAdultOrResponsible) {
            fm.subscribeToTopic(TOPIC_ADULT_PARENT)
                .addOnSuccessListener { Log.d(TAG, "Subscribed to $TOPIC_ADULT_PARENT") }
                .addOnFailureListener { e -> Log.e(TAG, "Sub fail", e) }
        } else {
            fm.unsubscribeFromTopic(TOPIC_ADULT_PARENT)
                .addOnSuccessListener { Log.d(TAG, "Unsubscribed from $TOPIC_ADULT_PARENT") }
                .addOnFailureListener { e -> Log.e(TAG, "Unsub fail", e) }
        }
    }
}
