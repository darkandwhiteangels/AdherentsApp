package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.data.model.dto.toDto
import com.antechrist.adherentsapp.data.model.dto.toDomain
import com.antechrist.adherentsapp.data.remote.InfoMessageRemoteDataSource
import com.antechrist.adherentsapp.domain.model.Audience
import com.antechrist.adherentsapp.domain.model.InfoMessage
import com.antechrist.adherentsapp.domain.repository.InfoMessagesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InfoMessagesRepositoryImpl @Inject constructor(
    private val remote: InfoMessageRemoteDataSource
) : InfoMessagesRepository {

    /**
     * Version alignée avec notre ViewModel actuel :
     * on reçoit le set d'audience de l'utilisateur courant
     * (ex: {ADULTS_ONLY, ALL_REGISTERED}) et on filtre après lecture Firestore.
     */
    override fun listenMessagesForAudience(
        audience: Set<Audience>
    ): Flow<List<InfoMessage>> {
        return remote.listenActiveMessages()
            .map { dtoList ->
                dtoList
                    .mapNotNull { it.toDomain() }
                    .filter { msg ->
                        // Règles de visibilité :
                        // - si le message est ALL_REGISTERED : tout le monde le voit
                        // - sinon, il doit y avoir intersection entre
                        //   l'audience du message et le set d'audience utilisateur
                        msg.audience == Audience.ALL_REGISTERED ||
                                audience.contains(msg.audience)
                    }
            }
    }

    /**
     * Publier un nouveau message dans Firestore.
     * Retourne l'id Firestore généré.
     */
    override suspend fun publishMessage(message: InfoMessage): String {
        val dto = message.toDto()
        android.util.Log.d(
            "InfoRepo",
            "publishMessage() dto= " +
                    "{title='${dto.title}', bodyLen=${dto.body?.length}, createdAt=${dto.createdAt}, " +
                    "createdByUid='${dto.createdByUid}', audience='${dto.audience}', active=${dto.active}}"
        )
        return remote.addMessage(dto)
    }

    /**
     * Rendre inactif (active=false).
     */
    override suspend fun deactivateMessage(messageId: String) {
        remote.deactivateMessage(messageId)
    }

    override suspend fun editMessage(
        messageId: String,
        title: String,
        body: String,
        editorUid: String
    ) {
        // Check 24h (côté client). La vraie sécurité reste côté Firestore rules.
        val createdAt = remote.getCreatedAtMillis(messageId)
            ?: throw IllegalStateException("Message introuvable")

        val now = System.currentTimeMillis()
        val canEdit = (now - createdAt) <= 24L * 60L * 60L * 1000L
        if (!canEdit) throw IllegalStateException("Délai d’édition dépassé (24h)")

        remote.updateMessage(messageId, title.trim(), body.trim())
    }

    override suspend fun deleteMessage(messageId: String) {
        remote.deleteMessage(messageId)
    }

}

