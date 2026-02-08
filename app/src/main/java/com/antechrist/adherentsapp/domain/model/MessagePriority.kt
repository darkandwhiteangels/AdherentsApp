package com.antechrist.adherentsapp.domain.model

/**
 * Priorité d'un message d'information.
 *
 * Utilisée pour :
 * - affichage visuel (couleurs, badges)
 * - tri éventuel
 * - mise en avant des messages importants
 */
enum class MessagePriority {
    LOW,        // Information mineure
    NORMAL,     // Information standard
    HIGH,       // Information importante
    URGENT      // Information critique / urgente
}
