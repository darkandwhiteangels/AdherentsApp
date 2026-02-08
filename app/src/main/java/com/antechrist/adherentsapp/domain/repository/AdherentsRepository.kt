package com.antechrist.adherentsapp.domain.repository

import com.antechrist.adherentsapp.domain.model.Adherent
import kotlinx.coroutines.flow.Flow

interface AdherentsRepository {
    suspend fun archiveAdherent(id: String)
    fun streamAll(): Flow<List<Adherent>>
    fun streamForGuardian(guardianId: String): Flow<List<Adherent>>
    suspend fun add(adherent: Adherent)
    suspend fun update(adherent: Adherent)
    suspend fun delete(id: String)
    fun streamById(id: String): Flow<Adherent?>
    suspend fun existsByEmail(email: String, excludeId: String? = null): Boolean // ✅
    fun streamByGroup(groupe: String): Flow<List<Adherent>>
    /**
     * ✅ Ajout pour l’export PDF :
     * Récupère nom/prénom pour un ensemble d’IDs d’adhérents.
     * Retourne Map<adherentId, Pair<Nom, Prenom>>.
     */
    suspend fun getMemberNamesByIds(ids: Set<String>): Map<String, Pair<String, String>>
    suspend fun setAdherentPhoto(adherentId: String, downloadUrl: String, updatedAt: Long)
}
