// app/src/main/java/.../domain/usecase/GetAdherentStreamByIdUseCase.kt

package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAdherentStreamByIdUseCase @Inject constructor(
    private val repo: AdherentsRepository
) {
    operator fun invoke(id: String): Flow<Adherent?> = repo.streamById(id)
}
