// com/antechrist/adherentsapp/data/seed/MovementSeed.kt
package com.antechrist.adherentsapp.data.seed

import com.antechrist.adherentsapp.domain.model.MovementCategory
import com.antechrist.adherentsapp.domain.model.MovementDirection
import com.antechrist.adherentsapp.domain.model.MovementRef
import javax.inject.Inject
import javax.inject.Singleton

interface MovementSeedProvider {
    fun getSeed(): List<MovementRef>
}

@Singleton
class MovementSeed @Inject constructor() : MovementSeedProvider {
    override fun getSeed(): List<MovementRef> = listOf(
        MovementRef(
            id = "MOV.STATIC",
            nameFr = "Sur place",
            nameJa = "Sonoba",
            category = MovementCategory.STATIC,
            angleDeg = 0,
            direction = MovementDirection.NEUTRE
        ),
        MovementRef(
            id = "MOV.AYUMI_ASHI_FORWARD",
            nameFr = "Avancer (ayumi-ashi)",
            nameJa = "Ayumi Ashi",
            category = MovementCategory.STEP,
            angleDeg = 0,
            direction = MovementDirection.AVANT
        ),
        MovementRef(
            id = "MOV.HIKI_ASHI_BACKWARD",
            nameFr = "Reculer (hiki-ashi)",
            nameJa = "Hiki Ashi",
            category = MovementCategory.STEP,
            angleDeg = 180,
            direction = MovementDirection.ARRIERE
        ),
        MovementRef(
            id = "MOV.YORI_ASHI_FORWARD",
            nameFr = "Yori-ashi avant",
            nameJa = "Yori ashi ↑",
            category = MovementCategory.SLIDE,
            angleDeg = 0,
            direction = MovementDirection.AVANT
        ),
        MovementRef(
            id = "MOV.YORI_ASHI_BACKWARD",
            nameFr = "Yori-ashi arrière",
            nameJa = "Yori ashi ↓",
            category = MovementCategory.SLIDE,
            angleDeg = 180,
            direction = MovementDirection.ARRIERE
        ),
        MovementRef(
            id = "MOV.TSUGI_ASHI_FORWARD",
            nameFr = "Pas glissé avant",
            nameJa = "Tsugi ashi ↑",
            category = MovementCategory.SLIDE,
            angleDeg = 0,
            direction = MovementDirection.AVANT
        ),
        MovementRef(
            id = "MOV.TSUGI_ASHI_BACKWARD",
            nameFr = "Pas glissé arrière",
            nameJa = "Tsugi ashi ↓",
            category = MovementCategory.SLIDE,
            angleDeg = 180,
            direction = MovementDirection.ARRIERE
        ),
//        MovementRef(
//            id = "MOV.MAWATTE_90",
//            nameFr = "Pivot 90°",
//            nameJa = "回って",
//            category = MovementCategory.PIVOT,
//            angleDeg = 90,
//            direction = MovementDirection.GAUCHE // ou DROITE selon l’usage
//        ),
        MovementRef(
            id = "MOV.MAWATTE_180",
            nameFr = "Demi tour",
            nameJa = "Mawatte",
            category = MovementCategory.PIVOT,
            angleDeg = 180,
            direction = MovementDirection.ARRIERE
        ),
        MovementRef(
            id = "MOV.NANAME_MAE",
            nameFr = "Diagonale avant",
            nameJa = "Naname mae",
            category = MovementCategory.STEP,
            angleDeg = 45,
            direction = MovementDirection.AVANT_DIAGONAL
        ),
        MovementRef(
            id = "MOV.NANAME_USHIRO",
            nameFr = "Diagonale arrière",
            nameJa = "Naname ushiro",
            category = MovementCategory.STEP,
            angleDeg = 135,
            direction = MovementDirection.ARRIERE_DIAGONAL
        )
    )
}
