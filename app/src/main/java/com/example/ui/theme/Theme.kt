package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ExolonDarkColorScheme =
  darkColorScheme(
    primary = SpectrumCyan,
    onPrimary = SpectrumVoid,
    primaryContainer = SpectrumSurfaceElevated,
    onPrimaryContainer = SpectrumCyan,
    secondary = SpectrumMagenta,
    onSecondary = SpectrumVoid,
    secondaryContainer = SpectrumSurfaceElevated,
    onSecondaryContainer = SpectrumMagenta,
    tertiary = SpectrumYellow,
    onTertiary = SpectrumVoid,
    background = SpectrumVoid,
    onBackground = SpectrumWhite,
    surface = SpectrumSurface,
    onSurface = SpectrumWhite,
    surfaceVariant = SpectrumSurfaceElevated,
    onSurfaceVariant = SpectrumMuted,
    error = SpectrumRed,
    onError = SpectrumWhite,
    outline = SpectrumBorder,
  )

val ExolonShapes =
  Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
  )

@Composable
fun ExolonTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = ExolonDarkColorScheme,
    typography = ExolonTypography,
    shapes = ExolonShapes,
    content = content,
  )
}
