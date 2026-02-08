package com.antechrist.adherentsapp.domain.repository

import com.antechrist.adherentsapp.domain.model.PresenceRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PresenceRepository {
    // --- APIs "objets" (existantes) ---
    fun stream(date: LocalDate, groupDisplay: String): Flow<Map<String, PresenceRecord>>
    suspend fun getDayGroup(date: LocalDate, groupDisplay: String): Map<String, PresenceRecord>

    // --- APIs "clés" (surcharges pour compat avec tes use-cases qui passent des String) ---
    fun stream(dateKey: String, groupKey: String): Flow<Map<String, PresenceRecord>>
    suspend fun getDayGroup(dateKey: String, groupKey: String): Map<String, PresenceRecord>

    // --- shim de compat pour SavePresenceForGroup (ne touche pas à l’enregistrement) ---
    suspend fun saveBatch(
        dateKey: String,
        groupKey: String,
        groupDisplay: String, // laissé pour compat si ton use-case le passe
        entries: Map<String, PresenceRecord>,
        updatedBy: String? = null
    )
}
