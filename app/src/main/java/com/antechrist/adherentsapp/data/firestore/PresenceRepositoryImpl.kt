package com.antechrist.adherentsapp.data.firestore

import com.antechrist.adherentsapp.domain.model.PresenceRecord
import com.antechrist.adherentsapp.domain.repository.PresenceRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class PresenceRepositoryImpl @Inject constructor(
    private val ds: PresenceDataSource
) : PresenceRepository {

    // ---- Objets ----
    override fun stream(date: LocalDate, groupDisplay: String): Flow<Map<String, PresenceRecord>> {
        val dateKey = ds.dateKey(date)
        val groupKey = ds.groupKey(groupDisplay)
        return ds.stream(dateKey, groupKey)
    }

    override suspend fun getDayGroup(
        date: LocalDate,
        groupDisplay: String
    ): Map<String, PresenceRecord> {
        val dateKey = ds.dateKey(date)
        val groupKey = ds.groupKey(groupDisplay)
        return ds.getDayGroupPresence(dateKey, groupKey)
    }

    // ---- Clés (compat avec tes use-cases qui passent des String) ----
    override fun stream(dateKey: String, groupKey: String): Flow<Map<String, PresenceRecord>> {
        return ds.stream(dateKey, groupKey)
    }

    override suspend fun getDayGroup(dateKey: String, groupKey: String): Map<String, PresenceRecord> {
        return ds.getDayGroupPresence(dateKey, groupKey)
    }

    // ---- Shim saveBatch (appelé par SavePresenceForGroup) ----
    override suspend fun saveBatch(
        dateKey: String,
        groupKey: String,
        groupDisplay: String,
        entries: Map<String, PresenceRecord>,
        updatedBy: String?
    ) {
        ds.saveBatch(
            dateKey = dateKey,
            groupKey = groupKey,
            updates = entries,
            overrideUpdatedBy = updatedBy
        )
    }
}