package com.antechrist.adherentsapp.domain.model

/**
 * Modèle métier pour un message d'information diffusé par le club.
 */
data class InfoMessage(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,        // epoch millis
    val createdByUid: String,   // UID Firebase Auth ou équivalent interne
    val audience: Audience,
    val active: Boolean,

    // 🆕 champs ajoutés (compatibles avec anciens docs Firestore)
    val priority: MessagePriority = MessagePriority.NORMAL,
    val color: String? = null,
    val targetGroupIds: List<String> = emptyList()
) {
    companion object {
        private const val EDIT_WINDOW_MS: Long = 24L * 60L * 60L * 1000L
    }

    fun canBeEdited(nowMillis: Long = System.currentTimeMillis()): Boolean =
        (nowMillis - createdAt) <= EDIT_WINDOW_MS

    fun hoursUntilEditExpires(nowMillis: Long = System.currentTimeMillis()): Long {
        val remaining = (createdAt + EDIT_WINDOW_MS) - nowMillis
        if (remaining <= 0) return 0
        return remaining / (60L * 60L * 1000L)
    }
}

/**
 * Portée du message (qui doit le voir / être notifié).
 */
enum class Audience {
    ADULTS_ONLY,
    GUARDIANS,
    ALL_REGISTERED,
    // 🆕 pour évolution "groupes"
    CUSTOM_GROUPS
}
