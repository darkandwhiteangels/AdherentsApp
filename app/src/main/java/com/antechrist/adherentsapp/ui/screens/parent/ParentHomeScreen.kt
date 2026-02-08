package com.antechrist.adherentsapp.ui.screens.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentHomeScreen(onLogout: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Espace parent") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Compte parent activé ✅")
            Text("L’espace parent arrive bientôt.")
            Button(onClick = onLogout) { Text("Se déconnecter") }
        }
    }
}
