package com.antechrist.adherentsapp.domain.editor

import com.antechrist.adherentsapp.domain.model.kihon.*

/** État minimal nécessaire au nouvel éditeur token-by-token. */
data class EditorState(
    val draft: SequenceDraft,
    val catalog: CatalogState,      // injecté/observé (techniques/movements/options/levels)
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isDirty: Boolean = false
)

/** Snapshot catalogue pour la résolution UI (IDs -> labels). */
data class CatalogState(
    val isReady: Boolean,
    val techniques: Map<String, TechniqueUiRef> = emptyMap(),
    val movements: Map<String, MovementUiRef> = emptyMap(),
    val options: Map<String, OptionUiRef> = emptyMap(),
    val levels: Map<String, LevelUiRef> = emptyMap()
)

/** UI-Refs légers (provenant de Firestore) — labels FR/JA, tags, etc. */
data class TechniqueUiRef(val id: String, val nameFr: String, val nameJa: String?)
data class MovementUiRef (val id: String, val nameFr: String, val nameJa: String?)
data class OptionUiRef   (val id: String, val nameFr: String, val nameJa: String?, val group: String)
data class LevelUiRef    (val id: String, val label: String, val nameJa: String?)

/** Intents (événements) que la VM devra réduire en nouvel état. */
sealed interface EditorIntent {
    /** Insère un token au cursorIndex puis décale le curseur d’un cran. */
    data class AddToken(val type: TokenType, val refId: String, val meta: Map<String, Any?>? = null) : EditorIntent

    /** Insertion en bloc à l’index donné (import/coller). */
    data class InsertTokens(val index: Int, val tokens: List<Token>) : EditorIntent

    /** Remplace le refId d’un token existant (tokenId inchangé, ordre conservé). */
    data class ReplaceToken(val tokenId: String, val newRefId: String, val newMeta: Map<String, Any?>? = null) : EditorIntent

    /** Supprime un token (reindex + recadrage du curseur). */
    data class RemoveToken(val tokenId: String) : EditorIntent

    /** Déplacement DnD: retire puis réinsère au nouvel index. */
    data class MoveToken(val tokenId: String, val toIndex: Int) : EditorIntent

    /** Déplace le curseur (point d’insertion). */
    data class SetCursor(val index: Int) : EditorIntent

    /** Recalcule order = index pour 0..n-1 (garantie d’invariant). */
    data object Reindex : EditorIntent

    /** Préprend la position par défaut si la séquence est vide ou n’ouvre pas par une POSITION. */
    data object EnsureOpeningPosition : EditorIntent
}

/**
 * Contrat de la VM : on ne code pas encore l’implémentation.
 * On valide d’abord les signatures/intents et le cycle d’état.
 */
interface EditorViewModelContract {
    val state: kotlinx.coroutines.flow.StateFlow<EditorState>

    fun dispatch(intent: EditorIntent)

    // Utilitaires pratiques que la BO pourra appeler (sugar over intents)
    fun addTechnique(refId: String) = dispatch(EditorIntent.AddToken(TokenType.TECHNIQUE, refId))
    fun addPosition(refId: String)  = dispatch(EditorIntent.AddToken(TokenType.POSITION,  refId))
    fun addMovement(refId: String)  = dispatch(EditorIntent.AddToken(TokenType.MOVEMENT,  refId))
    fun addOption(refId: String)    = dispatch(EditorIntent.AddToken(TokenType.OPTION,    refId))
    fun addLevel(refId: String)     = dispatch(EditorIntent.AddToken(TokenType.LEVEL,     refId))
}
