package com.antechrist.adherentsapp.backendtrigger

/**
 * Quand un admin crée un message, on veut aussi envoyer une notif FCM.
 * On va poster ça vers ta Cloud Function HTTPS (ou ton petit backend maison).
 * Je te donne juste la forme de la payload pour plus tard.
 */
data class CloudMessagePayload(
    val topic: String,          // ex "adult_parent"
    val title: String,          // ex "Cours annulé ce soir"
    val content: String         // ex "Le cours de 19h30 est annulé. Prof malade."
)
