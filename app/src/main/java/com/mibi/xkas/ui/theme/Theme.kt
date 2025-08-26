package com.mibi.xkas.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat


// Warna yang ingin dikolaborasikan
val SoftBlueGrayAccent = Color(0xFF81B2CA) // Biru Keabuan Lembut
val OnSoftBlueGrayAccent = Color.Black     // Teks di atasnya (bisa White jika SoftBlueGrayAccent lebih gelap)

// --- Warna Lembut untuk Light Theme ---
val CreamyBackgroundLight = Color(0xFFFAF7F2) // Warna ini akan kita gunakan untuk status bar
val SurfaceLight = Color(0xFFFFFFFF)
val DarkTextOnLight = Color(0xFF3C3B39)
val SubtleGrayTextLight = Color(0xFF7A7570)

// Warna Oranye Anda
val AppOrange = Color(0xFFFFA500)
val OnAppOrange = Color.Black // Atau Color.White jika lebih kontras

val mainContentCornerRadius = 24.dp
val MyButtonBlackBlue = Color(0xFF37474F)
val MyWhiteContent = Color.White
val MyButtonOrange = AppOrange

private val AppFixedLightColorScheme = lightColorScheme(
    primary = SoftBlueGrayAccent,
    onPrimary = OnSoftBlueGrayAccent,
    secondary = AppOrange,
    onSecondary = OnAppOrange,
    tertiary = SoftBlueGrayAccent,
    onTertiary = OnSoftBlueGrayAccent,
    background = CreamyBackgroundLight,
    onBackground = DarkTextOnLight,
    surface = SurfaceLight,
    onSurface = DarkTextOnLight,
    surfaceVariant = Color(0xFFDCEBF1),
    onSurfaceVariant = SubtleGrayTextLight,
    outline = Color(0xFFA8CADC),
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun XKasTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = AppFixedLightColorScheme
    val view = LocalView.current // Dapatkan LocalView

    if (!view.isInEditMode) { // Hanya jalankan SideEffect jika tidak dalam mode preview
        // Ambil window dari context view
        val window = (view.context as? Activity)?.window
        SideEffect {
            window?.let { // Pastikan window tidak null
                // Atur warna status bar
                it.statusBarColor = colorScheme.background.toArgb() // atau colorScheme.primary.toArgb()

                // Atur ikon status bar menjadi gelap karena background terang
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = true

                // Atur warna navigation bar (opsional, sesuaikan jika perlu)
                it.navigationBarColor = colorScheme.background.toArgb() // atau warna lain

                // Atur ikon navigation bar menjadi gelap
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}