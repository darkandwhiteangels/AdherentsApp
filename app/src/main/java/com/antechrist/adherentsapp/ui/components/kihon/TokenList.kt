package com.antechrist.adherentsapp.ui.components.kihon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antechrist.adherentsapp.domain.editor.CatalogState
import com.antechrist.adherentsapp.domain.editor.EditorState
import com.antechrist.adherentsapp.domain.model.kihon.Token
import com.antechrist.adherentsapp.domain.model.kihon.TokenType

/**
 * Affiche la séquence sous forme de liste ordonnée de tokens.
 * - key = tokenId (stable pour animations / DnD futur)
 * - actions : SetCursor (tap inter-éléments), Move (↑/↓), Remove (✖), Replace (tap sur le chip)
 *
 * NB: "replace" se contente d'émettre tokenId; la sélection du nouveau refId sera gérée par tes pickers.
 */
@Composable
fun TokenList(
    state: EditorState,
    onSetCursor: (index: Int) -> Unit,
    onMove: (tokenId: String, toIndex: Int) -> Unit,
    onRemove: (tokenId: String) -> Unit,
    onReplaceRequest: (tokenId: String, type: TokenType) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = state.draft.tokens
    val catalog = state.catalog

    Column(modifier) {
        // Bandeau d’infos léger
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val counts = tokens.groupBy { it.type }.mapValues { it.value.size }
            Text(
                text = "Séquence · ${tokens.size} item(s)  •  " +
                        "Pos:${counts[TokenType.POSITION] ?: 0}  " +
                        "Mvt:${counts[TokenType.MOVEMENT] ?: 0}  " +
                        "Tech:${counts[TokenType.TECHNIQUE] ?: 0}  " +
                        "Opt:${counts[TokenType.OPTION] ?: 0}  " +
                        "Lvl:${counts[TokenType.LEVEL] ?: 0}",
                style = MaterialTheme.typography.bodySmall
            )
            if (!catalog.isReady) {
                Text("Catalogue en chargement…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Zone d’insertion "avant le premier"
        CursorSlot(index = 0, isActive = state.draft.cursorIndex == 0, onSetCursor = onSetCursor)

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(tokens, key = { _, t -> t.tokenId }) { index, token ->
                TokenRow(
                    token = token,
                    label = resolveLabel(token, catalog),
                    onMoveUp = { if (index > 0) onMove(token.tokenId, index - 1) },
                    onMoveDown = { if (index < tokens.lastIndex) onMove(token.tokenId, index + 1) },
                    onRemove = { onRemove(token.tokenId) },
                    onReplace = { onReplaceRequest(token.tokenId, token.type) }
                )
                // Slot d’insertion entre éléments
                CursorSlot(
                    index = index + 1,
                    isActive = state.draft.cursorIndex == index + 1,
                    onSetCursor = onSetCursor
                )
            }
        }
    }
}

@Composable
private fun TokenRow(
    token: Token,
    label: String,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onReplace: () -> Unit
) {
    ElevatedCard {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index visuel
            Text(
                text = (token.order + 1).toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(28.dp)
            )

            // Type (chip)
            AssistChip(
                onClick = {},
                label = { Text(token.type.name.lowercase().replaceFirstChar { it.titlecase() }) },
                modifier = Modifier.padding(end = 8.dp),
            )

            // Label résolu (tap = replace)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).clickable(onClick = onReplace),
                maxLines = 2
            )

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, contentDescription = "Monter") }
                IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre") }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Supprimer") }
            }
        }
    }
}

@Composable
private fun CursorSlot(
    index: Int,
    isActive: Boolean,
    onSetCursor: (index: Int) -> Unit
) {
    val alpha = if (isActive) 1f else 0.25f
    Box(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onSetCursor(index) }
            .alpha(alpha)
    )
}

/** Résout un label FR à partir du catalogue; sinon affiche un placeholder explicite. */
private fun resolveLabel(token: Token, catalog: CatalogState): String {
    if (!catalog.isReady) return "(${token.refId})"
    return when (token.type) {
        TokenType.TECHNIQUE -> catalog.techniques[token.refId]?.nameFr
        TokenType.POSITION  -> catalog.techniques[token.refId]?.nameFr // positions sont des techniques kind=POSITION
        TokenType.MOVEMENT  -> catalog.movements[token.refId]?.nameFr
        TokenType.OPTION    -> catalog.options[token.refId]?.nameFr
        TokenType.LEVEL     -> catalog.levels[token.refId]?.label
    } ?: "⚠ manquant: ${token.refId}"
}
