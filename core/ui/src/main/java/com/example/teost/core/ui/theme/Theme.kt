package com.example.teost.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
// import com.google.accompanist.systemuicontroller.rememberSystemUiController // Deprecated and removed

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8DCDFF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFCAE6FF),

    secondary = Color(0xFFB7C9D9),
    onSecondary = Color(0xFF22333E),
    secondaryContainer = Color(0xFF384956),
    onSecondaryContainer = Color(0xFFD3E5F5),

    tertiary = Color(0xFFCFBFE6),
    onTertiary = Color(0xFF362B4A),
    tertiaryContainer = Color(0xFF4D4162),
    onTertiaryContainer = Color(0xFFEBDDFF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF001E2E),
    onBackground = Color(0xFFC7E6FF),

    surface = Color(0xFF001E2E),
    onSurface = Color(0xFFC7E6FF),
    surfaceVariant = Color(0xFF41484D),
    onSurfaceVariant = Color(0xFFC1C7CE),

    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41484D),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFC7E6FF),
    inverseOnSurface = Color(0xFF002E42),
    inversePrimary = Color(0xFF006493),
)

private val LightColorScheme = lightColorScheme(
    primary = TencentBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SecondaryBlue,
    onPrimaryContainer = TencentBlueDark,

    secondary = Color(0xFF50606E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E5F5),
    onSecondaryContainer = Color(0xFF0C1D29),

    tertiary = Color(0xFF65587B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF201634),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFAFCFF),
    onBackground = Color(0xFF001E2E),

    surface = Color(0xFFFAFCFF),
    onSurface = Color(0xFF001E2E),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),

    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CE),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF002E42),
    inverseOnSurface = Color(0xFFE5F1FB),
    inversePrimary = Color(0xFF8DCDFF),
)

@Composable
fun EdgeOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Enforce light theme only as per product requirement
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// Legacy theme name for compatibility
@Composable
fun TeostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    EdgeOneTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}