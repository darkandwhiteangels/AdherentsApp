package com.antechrist.adherentsapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

// Tes couleurs supplémentaires, thémables
data class ExtraColors(
    val certifiedBlue: Color,
    val rougeTatamie: Color,
    val grisDojo: Color,
    val chrome: Color,
    val greenTatamie: Color,
    val dorureDojo: Color,
    val warning: Color,
    val blanc: Color = Color.White,
    val noir: Color = Color.Black,
    val water: Color = Color(0x0000FFFF),
)

// Valeurs light/dark
private val LightExtraColors = ExtraColors(
    certifiedBlue = Blue40,
    rougeTatamie = Red40,
    grisDojo = Gray40,
    chrome = Chrome40,
    greenTatamie = Green40,
    dorureDojo = Dorure40,
    warning = Warning40,
    water = Water40,
)

private val DarkExtraColors = ExtraColors(
    certifiedBlue = Blue80,
    rougeTatamie = Red80,
    grisDojo = Gray80,
    chrome = Chrome80,
    greenTatamie = Green80,
    dorureDojo = Dorure80,
    warning = Warning80,
    water = Water80,
)

// Le CompositionLocal qui porte ces couleurs
private val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }

// Extension pratique: MaterialTheme.extraColors
val MaterialTheme.extraColors: ExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtraColors.current

@Composable
fun AdherentsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Choisit la palette extra selon le mode
    val extra = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }

}