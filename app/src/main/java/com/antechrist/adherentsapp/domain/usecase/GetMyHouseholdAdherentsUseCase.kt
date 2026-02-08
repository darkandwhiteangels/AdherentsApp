package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import com.antechrist.adherentsapp.core.debug.FireLog
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Récupère tous les adhérents du foyer du parent connecté.
 *
 * Utilise GetGuardianForCurrentUserUseCase pour obtenir le guardian du parent,
 * puis filtre les adhérents qui ont ce guardianId dans leur liste guardianIds.
 */
class GetMyHouseholdAdherentsUseCase @Inject constructor(
    private val getGuardianForCurrentUserUseCase: GetGuardianForCurrentUserUseCase,
    private val adherentsRepository: AdherentsRepository
) {
    operator fun invoke(): Flow<List<Adherent>> = flow {
        val TAG = "LoginTrace"

        val guardian = getGuardianForCurrentUserUseCase()
        if (guardian == null) {
            FireLog.step(TAG, "Household: guardian=null -> emit empty")
            emit(emptyList())
            return@flow
        }

        FireLog.step(TAG, "Household: guardianId=${guardian.id}")

        adherentsRepository.streamForGuardian(guardian.id)
            .onStart { FireLog.step(TAG, "Firestore LISTEN adherents WHERE guardianIds array-contains ${guardian.id} START") }
            .catch { e ->
                FireLog.err(TAG, "Firestore LISTEN adherents FAILED", e)
                emit(emptyList())
            }
            .collect { list ->
                FireLog.step(TAG, "Firestore LISTEN adherents OK size=${list.size}")
                emit(list)
            }
    }
}