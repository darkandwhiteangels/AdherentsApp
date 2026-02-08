package com.antechrist.adherentsapp.ui.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "onNewToken() = $token")

        // On délègue tout à FcmTokenManager
        // - Si aucun user connecté : il log un warning et ne fait rien
        // - Si user connecté : il enregistre sous users/{uid}/fcmTokens/{token}
        FcmTokenManager.saveTokenLocallyAndRemote(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "onMessageReceived() from=${remoteMessage.from}")

        val notifTitle = remoteMessage.notification?.title
            ?: remoteMessage.data["msgTitle"]
            ?: "Information club"

        val notifBody = remoteMessage.notification?.body
            ?: remoteMessage.data["msgBody"]
            ?: ""

        // Petite trace debug
        Log.d(
            "FCM",
            "Notify user: title=$notifTitle body=$notifBody data=${remoteMessage.data}"
        )

        // On montre la notif locale
        NotificationHelper.showInfoMessageNotification(
            context = this,
            title = notifTitle,
            body = notifBody
        )
    }
}
