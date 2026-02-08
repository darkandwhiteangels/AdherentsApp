package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.PresenceRecord
import com.antechrist.adherentsapp.domain.repository.PresenceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StreamPresenceForGroupDate @Inject constructor(
    private val repo: PresenceRepository
) {
    operator fun invoke(dateKey: String, groupKey: String): Flow<Map<String, PresenceRecord>> =
        repo.stream(dateKey, groupKey)
}
