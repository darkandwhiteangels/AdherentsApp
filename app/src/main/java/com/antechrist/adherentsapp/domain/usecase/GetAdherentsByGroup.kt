//package com.antechrist.adherentsapp.domain.usecase
//
//import com.antechrist.adherentsapp.domain.model.Adherent
//import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
//import kotlinx.coroutines.flow.Flow
//import javax.inject.Inject
//
//class GetAdherentsByGroup @Inject constructor(
//    private val repo: AdherentsRepository
//) {
//    operator fun invoke(groupe: String): Flow<List<Adherent>> = repo.streamByGroup(groupe)
//}
package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.domain.model.Adherent
import com.antechrist.adherentsapp.domain.repository.AdherentsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Rétro-compatible.
 *
 * - À court terme, on continue d’utiliser la route existante du repo (par libellé).
 * - Quand le repo saura requêter `whereArrayContains("groups", groupKey)`,
 *   on pourra ajouter un 2e flux et fusionner (OR logique) côté use case.
 */
class GetAdherentsByGroup @Inject constructor(
    private val repo: AdherentsRepository
) {
    operator fun invoke(groupeDisplay: String): Flow<List<Adherent>> =
        repo.streamByGroup(groupeDisplay)

    // (Option futur)
    // fun byGroupKey(groupKey: String): Flow<List<Adherent>> =
    //     repo.streamByGroupKey(groupKey)
}
