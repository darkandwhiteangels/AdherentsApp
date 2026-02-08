package com.antechrist.adherentsapp.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.app.PendingIntent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.antechrist.adherentsapp.MainActivity
import com.antechrist.adherentsapp.R

object NotificationHelper {

    const val CHANNEL_ID_INFO_MESSAGES = "info_messages_channel"
    private const val CHANNEL_NAME_INFO_MESSAGES = "Infos Club"
    private const val CHANNEL_DESC_INFO_MESSAGES =
        "Annonces importantes (annulation de cours, infos professeurs, etc.)"

    private var notifIdCounter = 1000

    fun createChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // évite de recréer le channel à chaque appel
        val existing = nm.getNotificationChannel(CHANNEL_ID_INFO_MESSAGES)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID_INFO_MESSAGES,
                CHANNEL_NAME_INFO_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_INFO_MESSAGES
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun showInfoMessageNotification(
        context: Context,
        title: String,
        body: String
    ) {
        // S'assurer que le channel existe (Android 8+)
        createChannel(context)

        // Vérif permission POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                return
            }
        }

        // Intent pour ouvrir l'app direct sur l'écran InfoMessages
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openDestination", "info_messages")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_INFO_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification_info) // ⚠ remplace par ton icône vectorielle notif
            .setContentTitle(title.ifBlank { "Information" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context)
                .notify(notifIdCounter++, builder.build())
        } catch (se: SecurityException) {
            // Permission pas accordée ou autre souci : on ignore pour ne pas crasher.
        }
    }
}
