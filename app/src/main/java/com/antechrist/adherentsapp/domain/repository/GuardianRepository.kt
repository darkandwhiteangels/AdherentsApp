package com.antechrist.adherentsapp.domain.repository

import com.antechrist.adherentsapp.domain.model.Guardian
import kotlinx.coroutines.flow.Flow

interface GuardiansRepository {
    suspend fun getById(id: String): Guardian?
    // Retrouver le guardian lié à un compte Firebase Auth (uid)
    suspend fun getByAuthUid(authUid: String): Guardian?
    fun streamAll(): Flow<List<Guardian>>
    suspend fun update(guardian: Guardian)
    // Trouver un responsable par email OU téléphone (digits only)
    suspend fun findByEmailOrTel(email: String?, telephoneDigits: String?): Guardian?
    // Créer un responsable et renvoyer son id
    // Lier un compte Firebase Auth (uid) à un guardian existant
    suspend fun linkAuthUid(guardianId: String, authUid: String)
    suspend fun createGuardian(
        nom: String,
        prenom: String,
        email: String?,
        telephoneDigits: String?,
        relation: String?
    ): String
    // Lier / Délier un responsable à un adhérent
    suspend fun linkGuardianToAdherent(
        adherentId: String,
        guardianId: String,
        setPrimaryIfEmpty: Boolean
    )
    suspend fun unlinkGuardianFromAdherent(
        adherentId: String,
        guardianId: String
    )
    // Définir un responsable principal pour un adhérent
    suspend fun setPrimaryGuardian(
        adherentId: String,
        guardianId: String
    )
    suspend fun acceptInvitation(inviteId: String, guardianId: String)

}
