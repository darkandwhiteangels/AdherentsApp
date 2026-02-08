package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAdherentsStreamUseCase @Inject constructor(
    private val repo: AdherentsRepository
) {
    operator fun invoke(): Flow<List<Adherent>> = repo.streamAll()
}
