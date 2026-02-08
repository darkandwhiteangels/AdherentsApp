package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.Guardian
import com.antechrist.adherentsapp.domain.repository.GuardiansRepository
import javax.inject.Inject

class FindGuardianByEmailOrTelUseCase @Inject constructor(
    private val repo: GuardiansRepository
) {
    suspend operator fun invoke(email: String?, telephoneDigits: String?): Guardian? {
        return repo.findByEmailOrTel(email, telephoneDigits)
    }
}
