package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),     // Vibrant lavender/purple
    onPrimary = Color(0xFF381E72),   // Dark purple
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),   // Soft grey purple
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF49454F), // Slate card borders/fill
    onSecondaryContainer = Color(0xFFE6E1E5),
    tertiary = Color(0xFFEFB8C8),    // Heartpink/empathy accent
    onTertiary = Color(0xFF492532),
    background = Color(0xFF1C1B1F),  // Jet black elegant base
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF2B2930),     // Medium bento slate card
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF1D192B), // Deep coding block contrast
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F)
)

private val LightColorScheme = DarkColorScheme // Standardize on Premium Dark

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark theme for the Elegant Dark brand vibe
  dynamicColor: Boolean = false, // Disable dynamic colors so our brand isn't washed out
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
