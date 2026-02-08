// com/antechrist/adherentsapp/data/model/dto/KihonSequenceDto.kt
package com.antechrist.adherentsapp.data.model.dto

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class KihonSequenceDto(
    // Valeurs par défaut pour éviter les NPE lors de la (dé)sérialisation
    val id: String = "",
    val name: String = "",
    val gradeKey: String = "",
    val version: String = "",
    /** "DRAFT" | "READY" | "PUBLISHED" */
    val status: String = "DRAFT",

    val objective: String? = null,
    val tags: List<String> = emptyList(),
    val steps: List<KihonStepDto> = emptyList(),     // ✅ on garde tes DTO d'étapes tels quels

    @get:PropertyName("isExamRequired") @set:PropertyName("isExamRequired")
    var isExamRequired: Boolean = false,

    val authorId: String = "",
    /** millis epoch (Long) ou Timestamp côté règles -> ici Long */
    val createdAt: Long = 0L,
    val updatedAt: Long? = 0L,

    val publishedAt: Long? = null,
    val publishedBy: String? = null
)

/* ========================================================================== */
/* =============== Helpers d'écriture "safe" vers Firestore ==================*/
/* ========================================================================== */

/**
 * Map strictement conforme à tes règles (UNIQUEMENT les clés autorisées).
 * NB: Les règles ne valident pas la forme interne de "steps" -> on envoie la liste de DTO telle quelle.
 */
fun KihonSequenceDto.asFirestoreMap(): Map<String, Any?> = hashMapOf<String, Any?>(
    "id"             to id,
    "name"           to name,
    "gradeKey"       to gradeKey,
    "version"        to version,
    "status"         to status, // "DRAFT"|"READY"|"PUBLISHED"
    "objective"      to objective,
    "tags"           to tags,   // List<String>
    "steps"          to steps,  // ✅ List<KihonStepDto> direct (structure interne non contrôlée par les règles)
    "isExamRequired" to isExamRequired,
    "authorId"       to authorId,
    "createdAt"      to createdAt,
    "updatedAt"      to updatedAt,
    "publishedAt"    to publishedAt,
    "publishedBy"    to publishedBy
)

