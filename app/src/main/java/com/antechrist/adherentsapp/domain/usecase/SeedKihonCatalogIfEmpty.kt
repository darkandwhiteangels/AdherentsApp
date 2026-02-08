package com.antechrist.adherentsapp.domain.usecase

import com.antechrist.adherentsapp.core.CoroutineDispatchers
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.repository.KihonRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.antechrist.adherentsapp.data.seed.MovementSeedProvider
import com.antechrist.adherentsapp.data.seed.OptionSeedProvider
import com.antechrist.adherentsapp.data.seed.LevelSeedProvider

class SeedKihonCatalogIfEmpty @Inject constructor(
    private val repo: KihonRepository,
    private val dispatchers: CoroutineDispatchers,
    private val seedProvider: KihonSeedProvider,
    private val movementSeed: MovementSeedProvider,
    private val optionSeed: OptionSeedProvider,
    private val levelSeed: LevelSeedProvider
) {
    interface KihonSeedProvider {
        fun getSeed(): List<TechniqueRef>
    }

    // ⬇️ retourne désormais le total inséré sur 4 familles
    suspend operator fun invoke(force: Boolean = false): Result<Int> =
        withContext(dispatchers.io) {
            runCatching {
                var total = 0

                // Techniques
                if (force || repo.isCatalogEmpty()) {
                    total += repo.upsertTechniques(seedProvider.getSeed())
                }

                // ✅ Movements
                if (force || repo.isMovementCatalogEmpty()) {
                    total += repo.upsertMovements(movementSeed.getSeed())
                }

                // ✅ Options
                if (force || repo.isOptionCatalogEmpty()) {
                    total += repo.upsertOptions(optionSeed.getSeed())
                }

                // ✅ Levels
                if (force || repo.isLevelCatalogEmpty()) {
                    total += repo.upsertLevels(levelSeed.getSeed())
                }

                total
            }
        }
}
