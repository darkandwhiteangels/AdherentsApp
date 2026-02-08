package com.antechrist.adherentsapp

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.antechrist.adherentsapp.ui.navigation.AppNavHost
import com.antechrist.adherentsapp.ui.theme.AdherentsAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        //enableEdgeToEdge()

        // Laisse le contenu passer sous les barres système (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        //enableImmersiveMode()

        // 🔎 Récupère l'intention spéciale (vient de la notif)
        val deepLinkDest = intent?.getStringExtra("openDestination")

        setContent {
            AdherentsAppTheme {
                // ✅ On demande la permission de notifications (Android 13+) une fois au lancement
                NotificationPermissionWrapper {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        AppNavHost(startDeepLinkDestination = deepLinkDest) // NavHost central
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Ré-applique l’immersif après dialogues/clavier/gestes
        if (hasFocus) enableImmersiveMode()
    }

    private fun enableImmersiveMode() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Cache status + nav bars. Si tu veux cacher seulement la barre de navigation:
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun NotificationPermissionWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Launcher pour la demande de permission
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { /* On n'a rien de spécial à faire ici pour l'instant */ }
        )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    content()
}