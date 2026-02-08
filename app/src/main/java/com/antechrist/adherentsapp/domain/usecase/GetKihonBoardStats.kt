// com/antechrist/adherentsapp/usecase/GetKihonBoardStats.kt
package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.core.CoroutineDispatchers
import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetKihonBoardStats @Inject constructor(
    private val repo: KihonRepository,
    private val dispatchers: CoroutineDispatchers
) {
    data class BoardStats(
        val byStatus: Map<KihonSequence.Status, Long>,
        /** Clé "__ALL__" pour le total global si gradeKeys == null */
        val byGrade: Map<String, Long>
    )

    /**
     * Récupère:
     * - les compteurs par statut (Draft/Ready/Published)
     * - les compteurs par grade (ou total global si gradeKeys == null)
     */
    suspend operator fun invoke(gradeKeys: List<String>? = null): Result<BoardStats> =
        withContext(dispatchers.io) {
            runCatching {
                val status = repo.getBoardCountsByStatus()
                val grades = repo.countByGrade(gradeKeys)
                BoardStats(byStatus = status, byGrade = grades)
            }
        }
}
