package com.antechrist.adherentsapp.domain.model

import com.antechrist.adherentsapp.domain.model.kihon.*
import com.antechrist.adherentsapp.domain.model.kihon.OptionKind
import com.antechrist.adherentsapp.domain.model.kihon.ExecutingLimbRole
import com.antechrist.adherentsapp.domain.model.kihon.FootLanding
import com.antechrist.adherentsapp.domain.model.kihon.Height
import com.antechrist.adherentsapp.domain.model.kihon.KihonStep
import com.antechrist.adherentsapp.domain.model.kihon.Movement
import com.antechrist.adherentsapp.domain.model.kihon.Tempo

// ────────────────────────────────────────────────────────────────────────────────
// DraftToken : inchangé (tes tokens Movement / Position / Technique / Option)
// ────────────────────────────────────────────────────────────────────────────────

sealed interface DraftToken {
    data class MovementToken(val movement: Movement): DraftToken
    data class PositionToken(val position: TechniqueRef): DraftToken
    data class TechniqueToken(
        val technique: TechniqueRef,
        val height: Height? = null,
        val direction: Direction? = null,
        val executingLimbRole: ExecutingLimbRole? = null
    ): DraftToken
    data class OptionToken(val option: OptionKind, val value: Any?): DraftToken
}

// ────────────────────────────────────────────────────────────────────────────────
// StepDraft : ajoute un 'stamp' immuable pour forcer la distinction en Flow
// ────────────────────────────────────────────────────────────────────────────────

data class StepDraft(
    val tokens: List<DraftToken> = emptyList(),
    val selectedIndex: Int? = null,
    val stamp: Long = 0L // <-- nouveauté : change à chaque mutation
) {
    val isEmpty: Boolean get() = tokens.isEmpty()

    // À adapter à ta règle métier de "complet"
    val isComplete: Boolean get() {
        val hasMovement = tokens.any { it is DraftToken.MovementToken }
        val hasPosition = tokens.any { it is DraftToken.PositionToken }
        val hasTechnique = tokens.any { it is DraftToken.TechniqueToken }
        return hasMovement && hasPosition && hasTechnique
    }

    // Helpers "immutables" : toujours renvoyer une **nouvelle** instance avec stamp++
    private fun nextStamp(): Long = System.nanoTime()

    fun select(index: Int?): StepDraft =
        copy(selectedIndex = index, stamp = nextStamp())

    fun removeAt(index: Int): StepDraft {
        if (index !in tokens.indices) return this
        val next = tokens.toMutableList().also { it.removeAt(index) }.toList()
        // si on supprime l’élément sélectionné, on remet selectedIndex à null
        val nextSel = selectedIndex?.let { sel ->
            when {
                sel == index -> null
                sel > index  -> sel - 1
                else         -> sel
            }
        }
        return copy(tokens = next, selectedIndex = nextSel, stamp = nextStamp())
    }

    fun move(from: Int, to: Int): StepDraft {
        if (from !in tokens.indices || to !in tokens.indices) return this
        if (from == to) return this
        val m = tokens.toMutableList()
        val item = m.removeAt(from)
        m.add(to, item)
        val newSel = selectedIndex?.let { sel ->
            when {
                sel == from -> to
                from < sel && sel <= to -> sel - 1
                to <= sel && sel < from -> sel + 1
                else -> sel
            }
        }
        return copy(tokens = m.toList(), selectedIndex = newSel, stamp = nextStamp())
    }

    fun reset(): StepDraft = StepDraft(stamp = nextStamp())

    /**
     * Ajoute ou remplace selon le type du token et l’élément sélectionné :
     * - si selectedIndex pointe un token du même "groupe", on remplace
     * - sinon on **ajoute à la fin**
     */
    fun appendOrReplace(token: DraftToken): StepDraft {
        val mutable = tokens.toMutableList()

        fun sameGroup(a: DraftToken, b: DraftToken): Boolean = when {
            a is DraftToken.MovementToken  && b is DraftToken.MovementToken  -> true
            a is DraftToken.PositionToken  && b is DraftToken.PositionToken  -> true
            a is DraftToken.TechniqueToken && b is DraftToken.TechniqueToken -> true
            a is DraftToken.OptionToken    && b is DraftToken.OptionToken    -> (a.option == b.option)
            else -> false
        }

        selectedIndex?.let { sel ->
            if (sel in mutable.indices) {
                val cur = mutable[sel]
                if (sameGroup(cur, token)) {
                    // remplace sur place
                    mutable[sel] = mergeIfTechnique(cur, token)
                    return copy(tokens = mutable.toList(), stamp = nextStamp())
                }
            }
        }

        // pas de sélection compatible → append
        mutable.add(mergeIfTechnique(null, token))
        return copy(tokens = mutable.toList(), stamp = nextStamp())
    }

    /**
     * Si on empile des infos (HEIGHT, DIRECTION, EXECUTING_LIMB…) sur une Technique,
     * on les associe directement dans le TechniqueToken courant (si sélectionné).
     */
    private fun mergeIfTechnique(existing: DraftToken?, incoming: DraftToken): DraftToken {
        val base = (existing ?: incoming)
        if (base is DraftToken.TechniqueToken && incoming is DraftToken.OptionToken) {
            return when (incoming.option) {
                OptionKind.HEIGHT -> base.copy(height = incoming.value as? Height)
                OptionKind.DIRECTION -> base.copy(direction = incoming.value as? Direction)
                OptionKind.EXECUTING_LIMB -> base.copy(executingLimbRole = incoming.value as? ExecutingLimbRole)
                else -> base
            }
        }
        return incoming
    }

    // Conversion vers KihonStep (comme chez toi ; adapte si besoin)
    fun toKihonStep(startPos: TechniqueRef): KihonStep? {
        val movement = tokens.firstOrNull { it is DraftToken.MovementToken } as? DraftToken.MovementToken
        val position = tokens.firstOrNull { it is DraftToken.PositionToken } as? DraftToken.PositionToken
        val technique = tokens.firstOrNull { it is DraftToken.TechniqueToken } as? DraftToken.TechniqueToken

        if (movement == null || position == null || technique == null) return null

//        val footLanding = tokens.lastOrNull { it is DraftToken.OptionToken && it.option == OptionKind.FOOT_LANDING }
//            ?.let { (it as DraftToken.OptionToken).value as? FootLanding } ?: FootLanding.NONE
//        val exec = tokens.lastOrNull { it is DraftToken.OptionToken && it.option == OptionKind.EXECUTING_LIMB }
//            ?.let { (it as DraftToken.OptionToken).value as? ExecutingLimbRole } ?: ExecutingLimbRole.NONE
//        val tempo = tokens.lastOrNull { it is DraftToken.OptionToken && it.option == OptionKind.TEMPO }
//            ?.let { (it as DraftToken.OptionToken).value as? Tempo } ?: Tempo.NORMAL
//        val kiai = tokens.any { it is DraftToken.OptionToken && it.option == OptionKind.KIAI }
        val footLanding = tokens.asReversed()
            .firstOrNull { it is DraftToken.OptionToken && it.option == OptionKind.FOOT_LANDING }
            ?.let { (it as DraftToken.OptionToken).value as? FootLanding } ?: FootLanding.NONE

        val exec = tokens.asReversed()
            .firstOrNull { it is DraftToken.OptionToken && it.option == OptionKind.EXECUTING_LIMB }
            ?.let { (it as DraftToken.OptionToken).value as? ExecutingLimbRole } ?: ExecutingLimbRole.NONE

        val tempo = tokens.asReversed()
            .firstOrNull { it is DraftToken.OptionToken && it.option == OptionKind.TEMPO }
            ?.let { (it as DraftToken.OptionToken).value as? Tempo } ?: Tempo.NORMAL

        val kiai = tokens.any { it is DraftToken.OptionToken && it.option == OptionKind.KIAI }

        // 🆕 préférence "même bras / même jambe"
        val samePref = when {
            tokens.any { it is DraftToken.OptionToken && it.option == OptionKind.SAME_ARM } -> SameLimbPreference.SAME_ARM
            tokens.any { it is DraftToken.OptionToken && it.option == OptionKind.SAME_LEG } -> SameLimbPreference.SAME_LEG
            else -> SameLimbPreference.NONE
        }
        return KihonStep(
            startPosition = startPos,
            endPosition = position.position,
            movement = movement.movement,
            technique = technique.technique,
            height = technique.height,
            direction = technique.direction,
            executingLimbRole = exec,
            footLanding = footLanding,
            tempo = tempo,
            kiai = kiai,
            sameLimbPreference = samePref
        )
    }
}
