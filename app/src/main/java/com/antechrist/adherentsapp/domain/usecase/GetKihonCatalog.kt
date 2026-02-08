// com/antechrist/adherentsapp/usecase/GetKihonCatalog.kt
package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetKihonCatalog @Inject constructor(
    private val repo: KihonRepository
) {
    operator fun invoke(): Flow<Map<String, TechniqueRef>> = repo.streamCatalog()
}
