package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.core.CoroutineDispatchers
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpsertTechnique @Inject constructor(
    private val repo: KihonRepository,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(ref: TechniqueRef): Result<String> =
        withContext(dispatchers.io) { runCatching { repo.upsertTechnique(ref) } }
}