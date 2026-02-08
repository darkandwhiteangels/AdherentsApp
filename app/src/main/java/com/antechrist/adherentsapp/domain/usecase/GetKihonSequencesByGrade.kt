// com/antechrist/adherentsapp/usecase/GetKihonSequencesByGrade.kt
package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetKihonSequencesByGrade @Inject constructor(
    private val repo: KihonRepository
) {
    operator fun invoke(gradeKey: String): Flow<List<KihonSequence>> =
        repo.streamSequencesByGrade(gradeKey)
}
