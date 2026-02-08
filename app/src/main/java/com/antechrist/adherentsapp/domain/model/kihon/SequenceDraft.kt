package com.antechrist.adherentsapp.domain.model.kihon

/**
 * Vérité unique : une liste ordonnée de tokens.
 * Le cursorIndex indique où insérer via la barre d’outils.
 */
data class SequenceDraft(
    val sequenceId: String,
    val tokens: List<Token> = emptyList(),
    val cursorIndex: Int = 0,
    val version: String = KihonDefaults.CATALOG_VERSION,
    val title: String? = null,
    val notes: String? = null
) {
    /** Helpers immutables pour produire un nouvel état (implémentation VM à venir). */
    fun withTokens(newTokens: List<Token>, newCursor: Int? = null): SequenceDraft {
        val reindexed = newTokens.reindex()
        val cursor = (newCursor ?: cursorIndex).coerceIn(0, reindexed.size)
        return copy(tokens = reindexed, cursorIndex = cursor)
    }
}

/** Recalcule order == index pour 0..n-1 (invariant dur). */
fun List<Token>.reindex(): List<Token> =
    mapIndexed { idx, t -> t.copy(order = idx) }
