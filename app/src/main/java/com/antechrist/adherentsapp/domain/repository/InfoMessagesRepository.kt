package com.antechrist.adherentsapp.domain.repository

import com.antechrist.adherentsapp.domain.model.InfoMessage
import kotlinx.coroutines.flow.Flow

/**
 * Contrat domaine pour récupérer / publier des messages d'info.
 */
interface InfoMessagesRepository {

    /**
     * Flux temps réel de tous les messages actifs visibles pour cette audience.
     *
     * @param audience portée de l'utilisateur connecté (par ex:
     *  - si utilisateur est un adulte pratiquant -> ADULTS_ONLY ou ALL_REGISTERED
     *  - si utilisateur est un guardian       -> GUARDIANS ou ALL_REGISTERED
     *
     * L'implé décidera du filtrage côté client ou requête Firestore.
     */
    fun listenMessagesForAudience(audience: Set<com.antechrist.adherentsapp.domain.model.Audience>): Flow<List<InfoMessage>>

    /**
     * Création d'un nouveau message.
     * Retourne l'id généré.
     */
    suspend fun publishMessage(message: InfoMessage): String

    /**
     * (Optionnel) désactive un message existant sans le supprimer physiquement.
     */
    suspend fun deactivateMessage(messageId: String)

    suspend fun editMessage(
        messageId: String,
        title: String,
        body: String,
        editorUid: String
    )

    suspend fun deleteMessage(messageId: String)

}
