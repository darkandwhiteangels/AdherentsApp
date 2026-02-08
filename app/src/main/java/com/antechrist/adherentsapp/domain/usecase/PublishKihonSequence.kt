// com/antechrist/adherentsapp/usecase/PublishKihonSequence.kt
package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.core.CoroutineDispatchers
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PublishKihonSequence @Inject constructor(
    private val repo: KihonRepository,
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(sequenceId: String, actorUid: String): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching { repo.publishSequence(sequenceId, actorUid) }
        }
}
