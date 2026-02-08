// app/src/main/java/com/antechrist/adherentsapp/data/firestore/FirestoreMappersKihon.kt
package com.antechrist.adherentsapp.data.firestore

import android.util.Log
import com.antechrist.adherentsapp.data.model.dto.KihonSequenceDto
import com.antechrist.adherentsapp.data.model.dto.KihonStepDto
import com.antechrist.adherentsapp.data.model.Movement as MovementDto
import com.antechrist.adherentsapp.data.model.Height as HeightDto
import com.antechrist.adherentsapp.data.model.Direction as DirectionDto
import com.antechrist.adherentsapp.data.model.Tempo as TempoDto
import com.antechrist.adherentsapp.data.model.Side as SideDto
import com.antechrist.adherentsapp.data.model.Hand as HandDto
import com.antechrist.adherentsapp.data.model.Foot as FootDto
import com.antechrist.adherentsapp.data.model.TechniqueKind
import com.antechrist.adherentsapp.data.model.dto.TechniqueRefDto
import com.antechrist.adherentsapp.domain.model.KihonSequence
import com.antechrist.adherentsapp.domain.model.TechniqueRef
import com.antechrist.adherentsapp.domain.model.kihon.KihonStep
import com.antechrist.adherentsapp.domain.model.kihon.Movement
import com.antechrist.adherentsapp.domain.model.kihon.Height
import com.antechrist.adherentsapp.domain.model.kihon.Direction
import com.antechrist.adherentsapp.domain.model.kihon.Tempo
import com.antechrist.adherentsapp.domain.model.kihon.ExecutingLimbRole
import com.antechrist.adherentsapp.domain.model.kihon.FootLanding
import com.antechrist.adherentsapp.domain.model.kihon.ActiveFoot
import com.antechrist.adherentsapp.domain.model.MovementRef
import com.antechrist.adherentsapp.domain.model.MovementCategory
import com.antechrist.adherentsapp.domain.model.MovementDirection
import com.antechrist.adherentsapp.data.model.dto.MovementRefDto
import com.antechrist.adherentsapp.domain.model.OptionRef
import com.antechrist.adherentsapp.domain.model.OptionGroup
import com.antechrist.adherentsapp.data.model.dto.OptionRefDto
import com.antechrist.adherentsapp.domain.model.LevelRef
import com.antechrist.adherentsapp.data.model.dto.LevelRefDto

private const val TAG_KIHON_MAP = "KihonMapper"
/* ──────────────────────────────────────────────
 * TECHNIQUE REF
 * ────────────────────────────────────────────── */

fun TechniqueRefDto.toDomainStrict(): TechniqueRef =
    TechniqueRef(
        id = requireNotNull(id) { "TechniqueRefDto.id is null" },
        kind = requireNotNull(kind) { "TechniqueRefDto.kind is null" }.toDomain(),
        nameJa = requireNotNull(nameJa) { "TechniqueRefDto.nameJa is null" },
        nameFr = requireNotNull(nameFr) { "TechniqueRefDto.nameFr is null" },
        aliases = aliases ?: emptyList(),
        subType = subType,
        notes = notes
    )

fun TechniqueRef.toDto(): TechniqueRefDto =
    TechniqueRefDto(
        id = id,
        kind = kind.toDto(),
        nameJa = nameJa,
        nameFr = nameFr,
        aliases = aliases,     // ✅ jamais null ici
        subType = subType,
        notes = notes
    )


private fun TechniqueKind.toDomain(): TechniqueRef.Kind = when (this) {
    TechniqueKind.DEFENSE  -> TechniqueRef.Kind.DEFENSE
    TechniqueKind.PUNCH    -> TechniqueRef.Kind.PUNCH
    TechniqueKind.KICK     -> TechniqueRef.Kind.KICK
    TechniqueKind.POSITION -> TechniqueRef.Kind.POSITION
}

private fun TechniqueRef.Kind.toDto(): TechniqueKind = when (this) {
    TechniqueRef.Kind.DEFENSE  -> TechniqueKind.DEFENSE
    TechniqueRef.Kind.PUNCH    -> TechniqueKind.PUNCH
    TechniqueRef.Kind.KICK     -> TechniqueKind.KICK
    TechniqueRef.Kind.POSITION -> TechniqueKind.POSITION
}

/* ──────────────────────────────────────────────
 * ENUMS DTO ↔ DOMAIN
 * ────────────────────────────────────────────── */

private fun MovementDto.toDomain(): Movement = when (this) {
    MovementDto.FORWARD        -> Movement.FORWARD
    MovementDto.BACKWARD       -> Movement.BACKWARD
    MovementDto.LATERAL        -> Movement.LATERAL
    MovementDto.CHASSE_FORWARD -> Movement.CHASSE_FORWARD
    MovementDto.CHASSE_BACK    -> Movement.CHASSE_BACK
    MovementDto.TIRES_FORWARD  -> Movement.TIRES_FORWARD
    MovementDto.TIRES_BACK     -> Movement.TIRES_BACK
    MovementDto.PIVOT_90_IN    -> Movement.PIVOT_90_IN
    MovementDto.PIVOT_90_OUT   -> Movement.PIVOT_90_OUT
    MovementDto.PIVOT_180      -> Movement.PIVOT_180
}

private fun Movement.toDtoOrNull(): MovementDto? = when (this) {
    Movement.FORWARD        -> MovementDto.FORWARD
    Movement.BACKWARD       -> MovementDto.BACKWARD
    Movement.LATERAL        -> MovementDto.LATERAL
    Movement.CHASSE_FORWARD -> MovementDto.CHASSE_FORWARD
    Movement.CHASSE_BACK    -> MovementDto.CHASSE_BACK
    Movement.TIRES_FORWARD  -> MovementDto.TIRES_FORWARD
    Movement.TIRES_BACK     -> MovementDto.TIRES_BACK
    Movement.PIVOT_90_IN    -> MovementDto.PIVOT_90_IN
    Movement.PIVOT_90_OUT   -> MovementDto.PIVOT_90_OUT
    Movement.PIVOT_180      -> MovementDto.PIVOT_180
    Movement.NONE, Movement.ON_PLACE -> null // non représentés dans le DTO
}

private fun HeightDto.toDomain(): Height = when (this) {
    HeightDto.JODAN  -> Height.JODAN
    HeightDto.CHUDAN -> Height.CHUDAN
    HeightDto.GEDAN  -> Height.GEDAN
}

private fun Height?.toDtoOrNull(): HeightDto? = when (this) {
    Height.JODAN  -> HeightDto.JODAN
    Height.CHUDAN -> HeightDto.CHUDAN
    Height.GEDAN  -> HeightDto.GEDAN
    null          -> null
}

private fun DirectionDto.toDomain(): Direction = when (this) {
    DirectionDto.FORWARD  -> Direction.FORWARD
    DirectionDto.BACKWARD -> Direction.BACKWARD
    DirectionDto.LATERAL  -> Direction.NONE   // pas d’info gauche/droite dans le DTO
}

private fun Direction?.toDtoOrNull(): DirectionDto? = when (this) {
    Direction.FORWARD  -> DirectionDto.FORWARD
    Direction.BACKWARD -> DirectionDto.BACKWARD
    Direction.LEFT,
    Direction.RIGHT    -> DirectionDto.LATERAL
    Direction.PIVOT_IN,
    Direction.PIVOT_OUT,
    Direction.NONE,
    null               -> null // non représentables proprement côté DTO
}

private fun TempoDto.toDomain(): Tempo = when (this) {
    // Mapping heuristique (adapter au besoin UI) :
    // ONE_COUNT  -> exécution simple (rapide)
    // TWO_COUNT  -> cadence régulière
    // BURST      -> en rafale (très rapide)
    // SLOW_TO_FAST -> crescendo (enchaîné)
    TempoDto.ONE_COUNT    -> Tempo.RAPIDE
    TempoDto.TWO_COUNT    -> Tempo.NORMAL
    TempoDto.BURST        -> Tempo.RAPIDE
    TempoDto.SLOW_TO_FAST -> Tempo.ENCHAINE
}

private fun Tempo?.toDtoOrNull(): TempoDto? = when (this) {
    Tempo.LENT     -> TempoDto.TWO_COUNT      // approximation “lent mais compté”
    Tempo.NORMAL   -> TempoDto.TWO_COUNT
    Tempo.RAPIDE   -> TempoDto.ONE_COUNT
    Tempo.ENCHAINE -> TempoDto.SLOW_TO_FAST
    null           -> null
}

private fun HandDto?.toExecutingLimbRole(side: SideDto?): ExecutingLimbRole =
    when (this) {
        HandDto.FRONT -> ExecutingLimbRole.BRAS_AVANT
        HandDto.REAR  -> ExecutingLimbRole.BRAS_ARRIERE
        //HandDto.LEAD_LEG -> ExecutingLimbRole.LEAD_LEG
        null -> {
            // Pas d’info “hand” -> on n’infère pas LEFT/RIGHT (pas mappable 1:1)
            ExecutingLimbRole.NONE
        }
    }

private fun ExecutingLimbRole?.toHandDtoOrNull(): HandDto? = when (this) {
    ExecutingLimbRole.BRAS_AVANT -> HandDto.FRONT
    ExecutingLimbRole.BRAS_ARRIERE -> HandDto.REAR
    //ExecutingLimbRole.LEAD_LEG -> HandDto.LEAD_LEG
    // LEAD_LEG / TRAIL_LEG / BOTH / NONE -> pas de mapping côté DTO
    else -> null
}

private fun FootDto?.toLanding(): FootLanding = when (this) {
    FootDto.FRONT -> FootLanding.DEVANT
    FootDto.REAR  -> FootLanding.DERRIERE
    null          -> FootLanding.NONE
}

private fun FootLanding?.toFootDtoOrNull(): FootDto? = when (this) {
    FootLanding.DEVANT -> FootDto.FRONT
    FootLanding.DERRIERE  -> FootDto.REAR
    FootLanding.MEME_ENDROIT,
    FootLanding.NONE,
    null -> null
}

/* ──────────────────────────────────────────────
 * STEP DTO ↔ DOMAIN
 * ────────────────────────────────────────────── */

fun KihonStepDto.toDomainStrict(
    catalog: Map<String, TechniqueRef>
): KihonStep {
    // Ids "bruts" issus du DTO (peuvent être nulls si anciens docs)
    val startId = this.startPositionId
    val techId  = this.techniqueId

    // 1) Résolution startPosition avec fallback
    val start: TechniqueRef = when {
        startId.isNullOrBlank() -> {
            Log.w(TAG_KIHON_MAP, "toDomainStrict: startPositionId is null/blank → fallback Undefined")
            TechniqueRef.Undefined
        }
        else -> {
            val found = catalog[startId]
            if (found == null) {
                Log.w(TAG_KIHON_MAP, "toDomainStrict: startPosition '$startId' NOT in catalog → fallback Undefined")
                TechniqueRef.Undefined
            } else {
                found
            }
        }
    }

    // 2) Résolution technique avec fallback
    val tech: TechniqueRef = when {
        techId.isNullOrBlank() -> {
            Log.w(TAG_KIHON_MAP, "toDomainStrict: techniqueId is null/blank → fallback Undefined")
            TechniqueRef.Undefined
        }
        else -> {
            val found = catalog[techId]
            if (found == null) {
                Log.w(TAG_KIHON_MAP, "toDomainStrict: technique '$techId' NOT in catalog → fallback Undefined")
                TechniqueRef.Undefined
            } else {
                found
            }
        }
    }

    // 3) Construction du step domain (avec défauts stables)
    return KihonStep(
        startPosition = start,
        movement = (movement?.toDomain()) ?: Movement.ON_PLACE,
        technique = tech,
        executingLimbRole = hand.toExecutingLimbRole(side),
        activeFoot = ActiveFoot.NONE,               // pas de champ DTO -> défaut
        footLanding = foot.toLanding(),
        endPosition = start,                        // pas d’endPositionId dans le DTO
        height = height?.toDomain(),
        tempo = (tempo?.toDomain()) ?: Tempo.NORMAL,
        direction = (direction?.toDomain()) ?: Direction.NONE,
        kiai = false
    )
}

private fun KihonStep.toDto(index: Int): KihonStepDto =
    KihonStepDto(
        index = index,
        startPositionId = startPosition.id,
        techniqueId = technique.id,
        movement = movement.toDtoOrNull(),
        // pas d’endPositionId dans le DTO aujourd’hui
        // Executing limb role -> Hand FRONT/REAR si applicable, sinon null
        hand = when (executingLimbRole) {
            ExecutingLimbRole.BRAS_AVANT -> HandDto.FRONT
            ExecutingLimbRole.BRAS_ARRIERE  -> HandDto.REAR
            else -> null
        },
        // FootLanding FRONT/BACK sinon null
        foot = when (footLanding) {
            FootLanding.DEVANT -> FootDto.FRONT
            FootLanding.DERRIERE  -> FootDto.REAR
            else -> null
        },
        height = height?.let { when (it) {
            Height.JODAN  -> HeightDto.JODAN
            Height.CHUDAN -> HeightDto.CHUDAN
            Height.GEDAN  -> HeightDto.GEDAN
        }},
        direction = when (direction) {
            Direction.FORWARD  -> DirectionDto.FORWARD
            Direction.BACKWARD -> DirectionDto.BACKWARD
            Direction.LEFT,
            Direction.RIGHT,
            Direction.PIVOT_IN,
            Direction.PIVOT_OUT,
            Direction.NONE     -> null // pas d’équivalent propre côté DTO -> on n’écrit pas
            else -> null
        },
        tempo = when (tempo) {
            Tempo.LENT     -> TempoDto.TWO_COUNT
            Tempo.NORMAL   -> TempoDto.TWO_COUNT
            Tempo.RAPIDE   -> TempoDto.ONE_COUNT
            Tempo.ENCHAINE -> TempoDto.SLOW_TO_FAST
        }
        // repetitions / countLabels / cue -> laisses null si non utilisés
    )

/* ──────────────────────────────────────────────
 * SÉQUENCE DTO ↔ DOMAIN
 * ────────────────────────────────────────────── */

private fun statusStringToDomain(s: String): KihonSequence.Status = when (s.uppercase()) {
    "DRAFT"     -> KihonSequence.Status.DRAFT
    "READY"     -> KihonSequence.Status.READY
    "PUBLISHED" -> KihonSequence.Status.PUBLISHED
    else        -> KihonSequence.Status.DRAFT
}

private fun KihonSequence.Status.toDtoString(): String = this.name

fun KihonSequenceDto.toDomainStrict(
    catalog: Map<String, TechniqueRef>,
    docId: String?
): KihonSequence {
    val seqId = this.id.ifBlank {
        requireNotNull(docId) { "Sequence id is missing (both dto.id blank and docId null)" }
    }

    // On respecte l’ordre de la liste DTO.
    // Si index est présent, on l’utilise pour trier ; sinon ordre naturel.
    val stepsDomain = steps
        .sortedBy { it.index ?: Int.MAX_VALUE }
        .map { it.toDomainStrict(catalog) }

    Log.d(TAG_KIHON_MAP, "toDomainStrict(seq): id=${this.id} steps=${stepsDomain.size} grade=${this.gradeKey} status=${this.status}")

    return KihonSequence(
        id = seqId,
        name = this.name,
        gradeKey = this.gradeKey,
        version = this.version,
        status = statusStringToDomain(this.status),
        objective = this.objective,
        tags = this.tags,
        steps = stepsDomain,
        isExamRequired = this.isExamRequired,
        authorId = this.authorId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        publishedAt = this.publishedAt,
        publishedBy = this.publishedBy
    ).withNormalizedIndices()
}

fun KihonSequence.toDto(): KihonSequenceDto =
    KihonSequenceDto(
        id = id,
        name = name,
        gradeKey = gradeKey,
        version = version,
        status = status.toDtoString(),
        objective = objective,
        tags = tags,
        //steps = steps.map { it.toDto() },
        steps = steps.mapIndexed { i, s -> s.toDto(i) },
        isExamRequired = isExamRequired,
        authorId = authorId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        publishedAt = publishedAt,
        publishedBy = publishedBy
    )

fun KihonSequenceDto.toFirestoreMap(): Map<String, Any?> = buildMap {
    put("id", id)
    put("name", name)
    put("gradeKey", gradeKey)
    put("version", version)
    put("status", status)
    objective?.let { put("objective", it) }
    put("tags", tags)
    put("steps", steps) // on pousse la liste de DTO telle quelle
    put("isExamRequired", isExamRequired)
    put("authorId", authorId)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    publishedAt?.let { put("publishedAt", it) }
    publishedBy?.let { put("publishedBy", it) }
}

fun MovementRefDto.toDomainStrict(): MovementRef =
    MovementRef(
        id = requireNotNull(id) { "MovementRefDto.id is null" },
        nameFr = requireNotNull(nameFr) { "MovementRefDto.nameFr is null" },
        nameJa = requireNotNull(nameJa) { "MovementRefDto.nameJa is null" },
        category = MovementCategory.valueOf(requireNotNull(category) { "MovementRefDto.category is null" }),
        angleDeg = angleDeg,
        direction = MovementDirection.valueOf(requireNotNull(direction) { "MovementRefDto.direction is null" })
    )

fun MovementRef.toDto(): MovementRefDto =
    MovementRefDto(
        id = id,
        nameFr = nameFr,
        nameJa = nameJa,
        category = category.name,
        angleDeg = angleDeg,
        direction = direction.name
    )

// ──────────────────────────────────────────────
// OPTION REF
// ──────────────────────────────────────────────

fun OptionRefDto.toDomainStrict(): OptionRef =
    OptionRef(
        id = requireNotNull(id) { "OptionRefDto.id is null" },
        group = OptionGroup.valueOf(requireNotNull(group) { "OptionRefDto.group is null" }),
        nameFr = requireNotNull(nameFr) { "OptionRefDto.nameFr is null" },
        nameJa = requireNotNull(nameJa) { "OptionRefDto.nameJa is null" },
        value = requireNotNull(value) { "OptionRefDto.value is null" }
    )

fun OptionRef.toDto(): OptionRefDto =
    OptionRefDto(
        id = id,
        group = group.name,
        nameFr = nameFr,
        nameJa = nameJa,
        value = value
    )

// ──────────────────────────────────────────────
// LEVEL REF
// ──────────────────────────────────────────────

fun LevelRefDto.toDomainStrict(): LevelRef =
    LevelRef(
        id = requireNotNull(id) { "LevelRefDto.id is null" },
        labelFr = requireNotNull(labelFr) { "LevelRefDto.labelFr is null" },
        nameJa = requireNotNull(nameJa) { "LevelRefDto.nameJa is null" },
        order = requireNotNull(order) { "LevelRefDto.order is null" },
        description = description
    )

fun LevelRef.toDto(): LevelRefDto =
    LevelRefDto(
        id = id,
        labelFr = labelFr,
        nameJa = nameJa,
        order = order,
        description = description
    )