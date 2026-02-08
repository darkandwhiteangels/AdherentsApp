package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.repository.GuardiansRepository
import javax.inject.Inject

class LinkAuthUidToGuardianUseCase @Inject constructor(
    private val repo: GuardiansRepository
) {
    suspend operator fun invoke(
        guardianId: String,
        authUid: String
    ) {
        repo.linkAuthUid(guardianId, authUid)
    }
}
