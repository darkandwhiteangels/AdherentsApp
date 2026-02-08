package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.PresenceRecord
import com.antechrist.adherentsapp.domain.repository.PresenceRepository
import javax.inject.Inject

class SavePresenceForGroupDate @Inject constructor(
    private val repo: PresenceRepository
) {
    suspend operator fun invoke(
        dateKey: String,
        groupKey: String,
        groupDisplay: String,
        entries: Map<String, PresenceRecord>
    ) = repo.saveBatch(dateKey, groupKey, groupDisplay, entries)
}
