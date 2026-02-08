package com.antechrist.adherentsapp.ui.screens.kihon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.editor.CatalogState
import com.antechrist.adherentsapp.domain.editor.EditorIntent
import com.antechrist.adherentsapp.domain.editor.EditorState
import com.antechrist.adherentsapp.domain.editor.EditorViewModelContract
import com.antechrist.adherentsapp.domain.model.kihon.KihonDefaults
import com.antechrist.adherentsapp.domain.model.kihon.SequenceDraft
import com.antechrist.adherentsapp.domain.model.kihon.Token
import com.antechrist.adherentsapp.domain.model.kihon.TokenType
import com.antechrist.adherentsapp.domain.model.kihon.reindex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * VM "token-by-token" (séquence unique) — implémente les reducers d'édition :
 * - Add / Insert / Replace / Remove / Move / SetCursor / Reindex / EnsureOpeningPosition
 * - Recalcule toujours order == index après mutation
 * - Maintient cursorIndex dans les bornes
 * - Marque isDirty sur toute mutation
 *
 * Le catalogue est injecté/updaté de l'extérieur via updateCatalog().
 */
class KihonSequenceEditorViewModelV2(
    sequenceId: String = UUID.randomUUID().toString(),
    initialCatalog: CatalogState = CatalogState(isReady = false),
    initialTokens: List<Token> = emptyList()
) : ViewModel(), EditorViewModelContract {

    private val _state = MutableStateFlow(
        EditorState(
            draft = SequenceDraft(
                sequenceId = sequenceId,
                tokens = initialTokens.reindex(),
                cursorIndex = 0
            ),
            catalog = initialCatalog,
            canUndo = false,
            canRedo = false,
            isDirty = false
        )
    )
    override val state: StateFlow<EditorState> = _state

    /** Appel externe pour pousser l'état catalogue (repo -> VM). */
    fun updateCatalog(newCatalog: CatalogState) {
        _state.value = _state.value.copy(catalog = newCatalog)
    }

    override fun dispatch(intent: EditorIntent) {
        viewModelScope.launch {
            reduce(intent)
        }
    }

    // ------------------------------
    // Reducers (mutations immuables)
    // ------------------------------

    private fun reduce(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.AddToken -> addToken(intent.type, intent.refId, intent.meta)
            is EditorIntent.InsertTokens -> insertTokens(intent.index, intent.tokens)
            is EditorIntent.ReplaceToken -> replaceToken(intent.tokenId, intent.newRefId, intent.newMeta)
            is EditorIntent.RemoveToken -> removeToken(intent.tokenId)
            is EditorIntent.MoveToken -> moveToken(intent.tokenId, intent.toIndex)
            is EditorIntent.SetCursor -> setCursor(intent.index)
            EditorIntent.Reindex -> reindexNow()
            EditorIntent.EnsureOpeningPosition -> ensureOpeningPosition()
        }
    }

    private fun snapshot() = _state.value
    private fun setState(newDraft: SequenceDraft, dirty: Boolean = true) {
        _state.value = _state.value.copy(draft = newDraft, isDirty = if (dirty) true else _state.value.isDirty)
    }

    // region Reducer impls

    private fun addToken(type: TokenType, refId: String, meta: Map<String, Any?>?) {
        var draft = snapshot().draft

        // Règle métier : si on insère le TOUT PREMIER token et que ce n'est pas une POSITION,
        // on préprend automatiquement la position d'ouverture (Hidari kamae).
        if (draft.tokens.isEmpty() && type != TokenType.POSITION) {
            draft = insertAt(
                draft = draft,
                indexRaw = 0,
                newTokens = listOf(
                    Token(
                        type = TokenType.POSITION,
                        refId = KihonDefaults.DEFAULT_OPENING_POSITION_ID
                    )
                ),
                moveCursor = true // après préprend, le curseur sera décalé
            )
        }

        val toInsert = Token(type = type, refId = refId, meta = meta)
        draft = insertAt(draft, draft.cursorIndex, listOf(toInsert), moveCursor = true)

        setState(draft)
    }

    private fun insertTokens(index: Int, tokens: List<Token>) {
        val draft = insertAt(snapshot().draft, index.coerceIn(0, snapshot().draft.tokens.size), tokens, moveCursor = false)
        setState(draft)
    }

    private fun replaceToken(tokenId: String, newRefId: String, newMeta: Map<String, Any?>?) {
        val prev = snapshot().draft
        val list = prev.tokens.map {
            if (it.tokenId == tokenId) it.copy(refId = newRefId, meta = newMeta) else it
        }.reindex()
        setState(prev.copy(tokens = list))
    }

    private fun removeToken(tokenId: String) {
        val prev = snapshot().draft
        val idx = prev.tokens.indexOfFirst { it.tokenId == tokenId }
        if (idx < 0) return

        val newList = prev.tokens.toMutableList().also { it.removeAt(idx) }.reindex()
        // Recalage du curseur
        val newCursor =
            if (prev.cursorIndex > idx) prev.cursorIndex - 1
            else prev.cursorIndex.coerceIn(0, newList.size)

        setState(prev.copy(tokens = newList, cursorIndex = newCursor))
    }

    private fun moveToken(tokenId: String, toIndexRaw: Int) {
        val prev = snapshot().draft
        val fromIndex = prev.tokens.indexOfFirst { it.tokenId == tokenId }
        if (fromIndex < 0) return

        val maxIndex = (prev.tokens.size - 1).coerceAtLeast(0)
        val toIndex = toIndexRaw.coerceIn(0, maxIndex)

        if (fromIndex == toIndex) return

        val moved = prev.tokens.toMutableList().apply {
            val t = removeAt(fromIndex)
            add(toIndex, t)
        }.reindex()

        val adjustedCursor = when {
            prev.cursorIndex == fromIndex -> toIndex
            fromIndex < prev.cursorIndex && prev.cursorIndex <= toIndex -> prev.cursorIndex - 1
            toIndex <= prev.cursorIndex && prev.cursorIndex < fromIndex -> prev.cursorIndex + 1
            else -> prev.cursorIndex
        }.coerceIn(0, moved.size)

        setState(prev.copy(tokens = moved, cursorIndex = adjustedCursor))
    }

    private fun setCursor(index: Int) {
        val prev = snapshot().draft
        val bounded = index.coerceIn(0, prev.tokens.size)
        if (bounded == prev.cursorIndex) return
        setState(prev.copy(cursorIndex = bounded), dirty = false)
    }

    private fun reindexNow() {
        val prev = snapshot().draft
        val re = prev.tokens.reindex()
        if (re === prev.tokens) return
        setState(prev.copy(tokens = re))
    }

    private fun ensureOpeningPosition() {
        val prev = snapshot().draft
        // Si vide -> injecter Hidari kamae
        if (prev.tokens.isEmpty()) {
            val injected = insertAt(
                draft = prev,
                indexRaw = 0,
                newTokens = listOf(Token(type = TokenType.POSITION, refId = KihonDefaults.DEFAULT_OPENING_POSITION_ID)),
                moveCursor = false
            )
            setState(injected)
            return
        }
        // Si le premier n'est pas une POSITION -> préprend Hidari kamae
        if (prev.tokens.first().type != TokenType.POSITION) {
            val injected = insertAt(
                draft = prev,
                indexRaw = 0,
                newTokens = listOf(Token(type = TokenType.POSITION, refId = KihonDefaults.DEFAULT_OPENING_POSITION_ID)),
                moveCursor = false
            )
            setState(injected)
        }
    }

    // endregion

    // ------------------------------
    // Helpers
    // ------------------------------

    /**
     * Insertion immuable d'une liste de tokens à un index donné, avec :
     * - reindex() global
     * - ajustement optionnel du curseur (si moveCursor = true ⇒ cursorIndex += newTokens.size si insertion à cursor)
     */
    private fun insertAt(
        draft: SequenceDraft,
        indexRaw: Int,
        newTokens: List<Token>,
        moveCursor: Boolean
    ): SequenceDraft {
        if (newTokens.isEmpty()) return draft

        val index = indexRaw.coerceIn(0, draft.tokens.size)
        val merged = buildList(draft.tokens.size + newTokens.size) {
            addAll(draft.tokens.subList(0, index))
            addAll(newTokens)
            addAll(draft.tokens.subList(index, draft.tokens.size))
        }.reindex()

        val newCursor = when {
            moveCursor && draft.cursorIndex >= index -> (draft.cursorIndex + newTokens.size)
            else -> draft.cursorIndex
        }.coerceIn(0, merged.size)

        return draft.copy(tokens = merged, cursorIndex = newCursor)
    }
}
