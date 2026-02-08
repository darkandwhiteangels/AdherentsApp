package com.antechrist.adherentsapp.ui.screens.bureau

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BureauHomeScreen(
    onOpenClubConfig: () -> Unit,
    onOpenAgCr: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val items = listOf(
        BureauItem(
            title = "Paramètres du club",
            subtitle = "Configuration générale",
            icon = Icons.Default.Settings,
            onClick = onOpenClubConfig
        ),
        BureauItem(
            title = "AG / Compte-rendu",
            subtitle = "Assemblée & CR",
            icon = Icons.Default.Description,
            onClick = onOpenAgCr
        ),
        BureauItem(
            title = "Stats",
            subtitle = "Module à venir",
            icon = Icons.Default.BarChart,
            onClick = onOpenStats
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bureau") },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
    ) { innerPaddings ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(innerPaddings)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                BureauSquareButton(item)
            }
        }
    }
}

@Composable
private fun BureauSquareButton(item: BureauItem) {
    Card(
        modifier = Modifier
            .aspectRatio(1f) // carré
            .clickable { item.onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall
            )
            if (item.subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private data class BureauItem(
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val onClick: () -> Unit
)
