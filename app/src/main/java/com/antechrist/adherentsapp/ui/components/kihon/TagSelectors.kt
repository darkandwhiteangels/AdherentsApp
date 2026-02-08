// com/antechrist/adherentsapp/ui/components/kihon/TagSelectors.kt
package com.antechrist.adherentsapp.ui.components.kihon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.model.kihon.Height
import com.antechrist.adherentsapp.domain.model.kihon.Direction
import com.antechrist.adherentsapp.domain.model.kihon.ExecutingLimbRole
import com.antechrist.adherentsapp.domain.model.kihon.FootLanding
import com.antechrist.adherentsapp.domain.model.kihon.Tempo
import com.antechrist.adherentsapp.domain.model.kihon.Movement

/* ──────────────────────────────────────────────
 * HEIGHT
 * ────────────────────────────────────────────── */
@Composable
fun HeightSelector(
    value: Height?,
    onChange: (Height?) -> Unit,
    modifier: Modifier = Modifier
) {
    ChipRow(modifier) {
        listOf(Height.JODAN, Height.CHUDAN, Height.GEDAN).forEach { h ->
            FilterChip(
                selected = value == h,
                onClick = { onChange(if (value == h) null else h) },
                label = { Text(heightLabel(h)) }
            )
        }
    }
}

fun heightLabel(h: Height): String = when (h) {
    Height.JODAN  -> "Jōdan"
    Height.CHUDAN -> "Chūdan"
    Height.GEDAN  -> "Gedan"
}

/* ──────────────────────────────────────────────
 * DIRECTION (domain: FORWARD/BACKWARD/LEFT/RIGHT/PIVOT_*[/NONE])
 * ────────────────────────────────────────────── */
@Composable
fun DirectionSelector(
    value: Direction?,
    onChange: (Direction?) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Direction.FORWARD,
        Direction.BACKWARD,
        Direction.LEFT,
        Direction.RIGHT,
        Direction.PIVOT_IN,
        Direction.PIVOT_OUT
    )
    ChipRow(modifier) {
        options.forEach { d ->
            FilterChip(
                selected = value == d,
                onClick = { onChange(if (value == d) null else d) },
                label = { Text(directionLabel(d)) }
            )
        }
    }
}

fun directionLabel(d: Direction): String = when (d) {
    Direction.FORWARD   -> "Avant"
    Direction.BACKWARD  -> "Arrière"
    Direction.LEFT      -> "Gauche"
    Direction.RIGHT     -> "Droite"
    Direction.PIVOT_IN  -> "Pivot (int.)"
    Direction.PIVOT_OUT -> "Pivot (ext.)"
    Direction.NONE      -> "—"
}

/* ──────────────────────────────────────────────
 * EXECUTING LIMB ROLE (remplace Side/Hand)
 * ────────────────────────────────────────────── */
@Composable
fun ExecutingLimbRoleSelector(
    value: ExecutingLimbRole,
    onChange: (ExecutingLimbRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        ExecutingLimbRole.NONE,
        ExecutingLimbRole.BRAS_AVANT,
        ExecutingLimbRole.BRAS_ARRIERE
        // Ajoute ici d'autres rôles si ton enum en a (LEAD_LEG, TRAIL_LEG, BOTH…)
    )
    ChipRow(modifier) {
        options.forEach { role ->
            FilterChip(
                selected = value == role,
                onClick = { onChange(role) },
                label = { Text(executingRoleLabel(role)) }
            )
        }
    }
}

fun executingRoleLabel(r: ExecutingLimbRole): String = when (r) {
    ExecutingLimbRole.NONE       -> "—"
    ExecutingLimbRole.BRAS_AVANT -> "Bras Avant"
    ExecutingLimbRole.BRAS_ARRIERE -> "Bras Arrière"
    //ExecutingLimbRole.LEAD_LEG   -> "Jambe directrice"
    //ExecutingLimbRole.TRAIL_LEG  -> "Jambe suiveuse"
    ExecutingLimbRole.BOTH       -> "Opposé"
    ExecutingLimbRole.JAMBE_AVANT -> "Jambe Avant"
    ExecutingLimbRole.JAMBE_ARRIERE -> "Jambe arrière"
}

/* ──────────────────────────────────────────────
 * FOOT LANDING (remplace Foot FRONT/REAR)
 * ────────────────────────────────────────────── */
@Composable
fun FootSelector(
    value: FootLanding,
    onChange: (FootLanding) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        FootLanding.NONE,
        FootLanding.DEVANT,
        FootLanding.DERRIERE,
        FootLanding.MEME_ENDROIT
    )
    ChipRow(modifier) {
        options.forEach { f ->
            FilterChip(
                selected = value == f,
                onClick = { onChange(f) },
                label = { Text(footLabel(f)) }
            )
        }
    }
}

fun footLabel(f: FootLanding): String = when (f) {
    FootLanding.NONE        -> "—"
    FootLanding.DEVANT       -> "Jambe AV"
    FootLanding.DERRIERE        -> "Jambe AR"
    FootLanding.MEME_ENDROIT  -> "Même place"
}

/* ──────────────────────────────────────────────
 * TEMPO (domain: LENT/NORMAL/RAPIDE/ENCHAINE)
 * ────────────────────────────────────────────── */
@Composable
fun TempoSelector(
    value: Tempo,
    onChange: (Tempo) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(Tempo.LENT, Tempo.NORMAL, Tempo.RAPIDE, Tempo.ENCHAINE)
    ChipRow(modifier) {
        options.forEach { t ->
            FilterChip(
                selected = value == t,
                onClick = { onChange(t) },
                label = { Text(tempoLabel(t)) }
            )
        }
    }
}

fun tempoLabel(t: Tempo): String = when (t) {
    Tempo.LENT     -> "Lent"
    Tempo.NORMAL   -> "Normal"
    Tempo.RAPIDE   -> "Rapide"
    Tempo.ENCHAINE -> "Enchaîné"
}

/* ──────────────────────────────────────────────
 * MOVEMENT (domain: inclut NONE/ON_PLACE/PIVOT_*…)
 * ────────────────────────────────────────────── */
@Composable
fun MovementSelector(
    value: Movement?,
    onChange: (Movement?) -> Unit,
    modifier: Modifier = Modifier
) {
    val all = listOf(
        Movement.ON_PLACE,
        Movement.FORWARD,
        Movement.BACKWARD,
        Movement.LATERAL,
        Movement.CHASSE_FORWARD,
        Movement.TIRES_FORWARD,
        Movement.CHASSE_BACK,
        Movement.TIRES_BACK,
        Movement.PIVOT_90_IN,
        Movement.PIVOT_90_OUT,
        Movement.PIVOT_180
    )
    ChipRow(modifier) {
        all.forEach { m ->
            FilterChip(
                selected = value == m,
                onClick = { onChange(if (value == m) null else m) },
                label = { Text(movementLabel(m)) }
            )
        }
    }
}

fun movementLabel(m: Movement): String = when (m) {
    Movement.NONE           -> "—"
    Movement.ON_PLACE       -> "Sur place"
    Movement.FORWARD        -> "⬆\uFE0F  Avancer"
    Movement.BACKWARD       -> "⬇\uFE0F  Reculer"
    Movement.LATERAL        -> "←\uFE0F Latéral"
    Movement.CHASSE_FORWARD -> "Pas chassé en ⬆\uFE0F"
    Movement.TIRES_FORWARD  -> "Pas tiré en ⬆\uFE0F"
    Movement.CHASSE_BACK    -> "Pas chassé en ⬇\uFE0F"
    Movement.TIRES_BACK     -> "Pas tiré en ⬇\uFE0F"
    Movement.PIVOT_90_IN    -> "Pivot 90° ↻"
    Movement.PIVOT_90_OUT   -> "Pivot 90° ↺"
    Movement.PIVOT_180      -> "Pivot 180°"
}

/* ──────────────────────────────────────────────
 * Helpers
 * ────────────────────────────────────────────── */
@Composable
private fun ChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}
