// com/antechrist/adherentsapp/ui/components/kihon/KihonStatusChip.kt
package com.antechrist.adherentsapp.ui.components.kihon

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.antechrist.adherentsapp.domain.model.KihonSequence

@Composable
fun KihonStatusChip(status: KihonSequence.Status) {
    val label = when (status) {
        KihonSequence.Status.DRAFT -> "Brouillon"
        KihonSequence.Status.READY -> "Prêt"
        KihonSequence.Status.PUBLISHED -> "Publié"
    }
    AssistChip(onClick = {}, label = { Text(label) })
}
