package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.core.CoroutineDispatchers
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpsertTechniques @Inject constructor(
    private val repo: KihonRepository,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(list: List<TechniqueRef>): Result<Int> =
        withContext(dispatchers.io) { runCatching { repo.upsertTechniques(list) } }
}