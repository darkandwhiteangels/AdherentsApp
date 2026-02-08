package com.antechrist.adherentsapp.ui.screens.access

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AccessDeniedScreen(onLogout: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Accès réservé aux administrateurs")
            Spacer(Modifier.size(12.dp))
            Button(onClick = onLogout) { Text("Se déconnecter") }
        }
    }
}
