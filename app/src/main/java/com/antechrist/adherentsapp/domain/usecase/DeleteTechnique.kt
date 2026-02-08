package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.core.CoroutineDispatchers
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteTechnique @Inject constructor(
    private val repo: KihonRepository,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(id: String): Result<Unit> =
        withContext(dispatchers.io) { runCatching { repo.deleteTechnique(id) } }
}
