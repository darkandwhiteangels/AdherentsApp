package com.antechrist.adherentsapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessDeniedScreen(onBack: () -> Unit) {      // ⬅️ AVOIR CE PARAM
    Scaffold(topBar = {
        TopAppBar(title = { Text("Accès refusé") })
    }) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Vous n’avez pas les droits pour cette section.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Retour") }           // ⬅️ UTILISÉ ICI
        }
    }
}
