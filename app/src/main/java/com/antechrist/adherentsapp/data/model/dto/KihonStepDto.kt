// com/antechrist/adherentsapp/data/model/dto/KihonStepDto.kt
package com.antechrist.adherentsapp.data.model.dto

import androidx.annotation.Keep
import com.antechrist.adherentsapp.data.model.Coordination
import com.antechrist.adherentsapp.data.model.Direction
import com.antechrist.adherentsapp.data.model.Foot
import com.antechrist.adherentsapp.data.model.Hand
import com.antechrist.adherentsapp.data.model.Height
import com.antechrist.adherentsapp.data.model.Movement
import com.antechrist.adherentsapp.data.model.Side
import com.antechrist.adherentsapp.data.model.Tempo

/**
 * Step DTO stocké dans un KihonSequenceDto.
 * On référence les techniques par leurs IDs pour éviter la duplication.
 */
@Keep
data class KihonStepDto(
    val index: Int? = null,                 // Ordre 0..n
    val startPositionId: String? = null,    // TechniqueRefDto.id (kind = POSITION)
    val movement: Movement? = null,         // Optionnel

    val techniqueId: String? = null,        // TechniqueRefDto.id (DEFENSE/POING/PIED)

    // Paramètres d'exécution
    val side: Side? = null,
    val hand: Hand? = null,
    val foot: Foot? = null,
    val height: Height? = null,
    val direction: Direction? = null,
    val coordination: Coordination? = null,
    val tempo: Tempo? = null,

    val repetitions: Int? = null,           // >=1
    val countLabels: List<String>? = null,  // ["ichi","ni","san"] si personnalisé
    val cue: String? = null                 // consignes (hanche, respiration, distance…)
)
