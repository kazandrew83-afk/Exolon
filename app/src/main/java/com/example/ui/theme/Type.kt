package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val ArcadePixelFont = FontFamily(
  Font(R.font.silkscreen, FontWeight.Normal),
  Font(R.font.silkscreen, FontWeight.Bold),
)

val SciFiUiFont = FontFamily(
  Font(R.font.chakra_petch, FontWeight.Normal),
  Font(R.font.chakra_petch, FontWeight.Medium),
  Font(R.font.chakra_petch, FontWeight.SemiBold),
  Font(R.font.chakra_petch, FontWeight.Bold),
)

val ExolonTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = ArcadePixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = 2.sp,
      ),
    displaySmall =
      TextStyle(
        fontFamily = ArcadePixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.5.sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = ArcadePixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 1.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = SciFiUiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = SciFiUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = SciFiUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = ArcadePixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = ArcadePixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp,
      ),
  )
