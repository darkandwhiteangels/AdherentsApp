// com/antechrist/adherentsapp/data/seed/OptionSeed.kt
package com.antechrist.adherentsapp.data.seed

import com.antechrist.adherentsapp.domain.model.OptionGroup
import com.antechrist.adherentsapp.domain.model.OptionRef
import javax.inject.Inject
import javax.inject.Singleton

interface OptionSeedProvider {
    fun getSeed(): List<OptionRef>
}

@Singleton
class OptionSeed @Inject constructor() : OptionSeedProvider {
    override fun getSeed(): List<OptionRef> = listOf(
        // SIDE
        OptionRef(
            id = "OPT.SIDE.LEFT",
            group = OptionGroup.SIDE,
            nameFr = "Gauche",
            nameJa = "Hidari",
            value = "gauche"
        ),
        OptionRef(
            id = "OPT.SIDE.RIGHT",
            group = OptionGroup.SIDE,
            nameFr = "Droite",
            nameJa = "Migi",
            value = "droite"
        ),

        // DIRECTION (modificateur optionnel, en plus du Movement)
        OptionRef(
            id = "OPT.DIRECTION.FORWARD",
            group = OptionGroup.DIRECTION,
            nameFr = "Vers l’avant",
            nameJa = "Mae",
            value = "avant"
        ),
        OptionRef(
            id = "OPT.DIRECTION.BACKWARD",
            group = OptionGroup.DIRECTION,
            nameFr = "Vers l’arrière",
            nameJa = "Ushiro",
            value = "arriere"
        ),
        OptionRef(
            id = "OPT.LIMB.ARM_FRONT",
            group = OptionGroup.LIMB,
            nameFr = "Bras avant",
            nameJa = "Mae no ude",
            value = "bras_avant"
        ),
        OptionRef(
            id = "OPT.LIMB.ARM_REAR",
            group = OptionGroup.LIMB,
            nameFr = "Bras arrière",
            nameJa = "Ushiro no ude",
            value = "bras_arriere"
        ),
        OptionRef(
            id = "OPT.LIMB.LEG_FRONT",
            group = OptionGroup.LIMB,
            nameFr = "Jambe avant",
            nameJa = "Mae ashi",
            value = "jambe_avant"
        ),
        OptionRef(
            id = "OPT.LIMB.LEG_REAR",
            group = OptionGroup.LIMB,
            nameFr = "Jambe arrière",
            nameJa = "Ushiro ashi",
            value = "jambe_arriere"
        ),

        // ─── REPOSE (placement après action) ───
        OptionRef(
            id = "OPT.REPOSE.FRONT",
            group = OptionGroup.REPOSE,
            nameFr = "Repose devant",
            nameJa = "Mae ni oku",
            value = "repose_devant"
        ),
        OptionRef(
            id = "OPT.REPOSE.BACK",
            group = OptionGroup.REPOSE,
            nameFr = "Repose derrière",
            nameJa = "Ushiro ni oku",
            value = "repose_derriere"
        ),

        // ─── RELATION (par rapport à la garde) ───
        OptionRef(
            id = "OPT.RELATION.GYAKU",
            group = OptionGroup.RELATION,
            nameFr = "Opposé",
            nameJa = "Gyaku",
            value = "oppose"
        ),
        OptionRef(
            id = "OPT.RELATION.SAME_ARM",
            group = OptionGroup.RELATION,
            nameFr = "Même bras",
            nameJa = "Onaji ude",
            value = "meme_bras"
        ),
        OptionRef(
            id = "OPT.RELATION.SAME_LEG",
            group = OptionGroup.RELATION,
            nameFr = "Même jambe",
            nameJa = "Onaji ashi",
            value = "meme_jambe"
        )
    )
}
