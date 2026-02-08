package com.antechrist.adherentsapp.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.messaging.FirebaseMessaging
import com.antechrist.adherentsapp.ui.notifications.FcmTokenManager
import android.util.Log

@Composable
fun LoginScreen(
    onAdmin: () -> Unit,
    onNonAdmin: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val nav by vm.nav.collectAsState()

    // ⚠ ICI : on réagit aux changements de nav
    LaunchedEffect(nav) {
        when (nav) {
            is LoginNav.GoList -> {
                // L'utilisateur est connecté et autorisé à aller sur la liste (staff etc.)
                // => on pousse le token FCM AVANT navigation
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        Log.d("LoginScreen", "Fetched FCM token after login (GoList): $token")
                        FcmTokenManager.saveTokenLocallyAndRemote(token)
                        // ensuite on reprend le flux normal
                        vm.consumeNav()
                        onAdmin()
                    }
                    .addOnFailureListener { e ->
                        Log.e("LoginScreen", "Failed to fetch FCM token after login (GoList)", e)
                        vm.consumeNav()
                        onAdmin()
                    }
            }

            is LoginNav.GoAccessDenied -> {
                // L'utilisateur est connecté mais pas autorisé "admin/staff"
                // => il peut quand même avoir du rôle parent / member
                // donc on enregistre AUSSI son token.
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        Log.d("LoginScreen", "Fetched FCM token after login (GoAccessDenied): $token")
                        FcmTokenManager.saveTokenLocallyAndRemote(token)
                        vm.consumeNav()
                        onNonAdmin()
                    }
                    .addOnFailureListener { e ->
                        Log.e("LoginScreen", "Failed to fetch FCM token after login (GoAccessDenied)", e)
                        vm.consumeNav()
                        onNonAdmin()
                    }
            }

            else -> Unit
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connexion admin",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = ui.email,
                onValueChange = vm::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.password,
                onValueChange = vm::onPasswordChange,
                label = { Text("Mot de passe") },
                singleLine = true,
                enabled = !ui.loading,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (ui.error != null) {
                Text(
                    ui.error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = vm::signIn,
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("Se connecter")
                }
            }
        }
    }
}
