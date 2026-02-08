// com/antechrist/adherentsapp/data/seed/LevelSeed.kt
package com.antechrist.adherentsapp.data.seed

import com.antechrist.adherentsapp.domain.model.LevelRef
import javax.inject.Inject
import javax.inject.Singleton

interface LevelSeedProvider {
    fun getSeed(): List<LevelRef>
}

@Singleton
class LevelSeed @Inject constructor() : LevelSeedProvider {
    override fun getSeed(): List<LevelRef> = listOf(
        LevelRef(
            id = "LVL.GEDAN",
            labelFr = "Niveau bas",
            nameJa = "Gedan",
            order = 1,
            description = "Coups vers les jambes"
        ),
        LevelRef(
            id = "LVL.CHUDAN",
            labelFr = "Niveau moyen",
            nameJa = "Chūdan",
            order = 2,
            description = "Coups vers le torse"
        ),
        LevelRef(
            id = "LVL.JODAN",
            labelFr = "Niveau haut",
            nameJa = "Jōdan",
            order = 3,
            description = "Coups vers la tête"
        )
    )
}
