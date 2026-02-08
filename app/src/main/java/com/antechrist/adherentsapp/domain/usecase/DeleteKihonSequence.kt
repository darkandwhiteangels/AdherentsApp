package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.repository.KihonRepository
import javax.inject.Inject

class DeleteKihonSequence @Inject constructor(
    private val repo: KihonRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = repo.deleteSequence(id)
}
