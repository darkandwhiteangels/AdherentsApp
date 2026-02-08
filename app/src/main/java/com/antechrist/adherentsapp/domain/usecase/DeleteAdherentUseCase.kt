package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import javax.inject.Inject

class DeleteAdherentUseCase @Inject constructor(
    private val repo: AdherentsRepository
) {
    suspend operator fun invoke(id: String) = repo.delete(id)
}
