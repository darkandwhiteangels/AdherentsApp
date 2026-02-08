package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import javax.inject.Inject

class AddAdherentUseCase @Inject constructor(
    private val repo: AdherentsRepository
) {
    suspend operator fun invoke(a: Adherent) = repo.add(a)
}
