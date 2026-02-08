package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.repository.GuardiansRepository
import javax.inject.Inject

class UnlinkGuardianFromAdherentUseCase @Inject constructor(
    private val repo: GuardiansRepository
) {
    suspend operator fun invoke(
        adherentId: String,
        guardianId: String
    ) = repo.unlinkGuardianFromAdherent(adherentId, guardianId)
}
