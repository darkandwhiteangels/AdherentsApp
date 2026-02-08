// com/antechrist/adherentsapp/usecase/SaveKihonSequence.kt
package com.antechrist.adherentsapp.domain.usecase

import android.util.Log
import com.antechrist.adherentsapp.core.CoroutineDispatchers
import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveKihonSequence @Inject constructor(
    private val repo: KihonRepository,
    private val dispatchers: CoroutineDispatchers
) {

    private companion object {
        private const val TAG = "SaveKihonSequenceUC"
    }

    /** Petit résumé lisible pour Logcat */
    private fun seqSummary(seq: KihonSequence): String =
        "id=${seq.id}, grade=${seq.gradeKey}, name='${seq.name}', steps=${seq.steps.size}, exam=${seq.isExamRequired}"

    suspend operator fun invoke(seq: KihonSequence): Result<Unit> =
        withContext(dispatchers.io) {
            Log.d(TAG, "invoke(): PREP ${seqSummary(seq)}")
            val t0 = System.currentTimeMillis()

            val res = runCatching { repo.saveSequence(seq) }
            val dt = System.currentTimeMillis() - t0

            res
                .onSuccess {
                    Log.d(TAG, "invoke(): OK (${dt}ms) ${seqSummary(seq)}")
                }
                .onFailure { e ->
                    Log.e(TAG, "invoke(): FAIL (${dt}ms) ${e.message} :: ${seqSummary(seq)}", e)
                }
        }
}
