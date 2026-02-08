package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.repository.GuardiansRepository
import javax.inject.Inject

class CreateGuardianUseCase @Inject constructor(
    private val repo: GuardiansRepository
) {
    suspend operator fun invoke(
        nom: String,
        prenom: String,
        email: String?,
        telephoneDigits: String?,
        relation: String?
    ): String = repo.createGuardian(nom, prenom, email, telephoneDigits, relation)
}
