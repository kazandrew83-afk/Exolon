package com.example.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameUiState
import com.example.game.model.TouchControlMode
import com.example.game.ui.canvas.ExolonGameCanvas
import com.example.ui.theme.SpectrumBorder
import com.example.ui.theme.SpectrumCyan
import com.example.ui.theme.SpectrumMagenta
import com.example.ui.theme.SpectrumMuted
import com.example.ui.theme.SpectrumSurface
import com.example.ui.theme.SpectrumSurfaceElevated
import com.example.ui.theme.SpectrumVoid
import com.example.ui.theme.SpectrumWhite
import com.example.ui.theme.SpectrumYellow

@Composable
fun TitleScreen(
  uiState: GameUiState,
  onStartNewGame: () -> Unit,
  onResumeGame: () -> Unit,
  onToggleSound: () -> Unit,
  onToggleCrt: () -> Unit,
  onToggleControlMode: () -> Unit,
) {
  var showScores by remember { mutableStateOf(false) }
  var showGuide by remember { mutableStateOf(false) }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(SpectrumVoid, SpectrumSurface, SpectrumVoid)
          )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier
          .widthIn(max = 560.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
      // ZX Spectrum Rainbow Bar
      Row(
        modifier = Modifier.fillMaxWidth(0.7f).height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(Modifier.weight(1f).height(6.dp).background(SpectrumCyan))
        Box(Modifier.weight(1f).height(6.dp).background(SpectrumYellow))
        Box(Modifier.weight(1f).height(6.dp).background(SpectrumMagenta))
        Box(Modifier.weight(1f).height(6.dp).background(SpectrumCyan))
      }

      Text(
        text = "EXOLON",
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp),
        color = SpectrumCyan,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "ZX SPECTRUM 1987 • KOTLIN TOUCH EDITION",
        style = MaterialTheme.typography.labelSmall,
        color = SpectrumYellow,
        textAlign = TextAlign.Center,
      )

      // Live preview frame of Sector 1
      ExolonGameCanvas(
        uiState = uiState,
        modifier = Modifier.fillMaxWidth().height(185.dp),
      )

      // Top score pill
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(SpectrumSurfaceElevated, RoundedCornerShape(10.dp))
            .border(1.dp, SpectrumBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "РЕКОРД: ${uiState.highScore.toString().padStart(6, '0')}",
          style = MaterialTheme.typography.labelLarge,
          color = SpectrumYellow,
        )
        Text(
          text = "5 СЕКТОРОВ • 30 ЭКРАНОВ",
          style = MaterialTheme.typography.labelSmall,
          color = SpectrumMuted,
        )
      }

      // Primary actions
      if (uiState.hasActiveSession) {
        Button(
          onClick = onResumeGame,
          colors = ButtonDefaults.buttonColors(containerColor = SpectrumYellow, contentColor = SpectrumVoid),
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("resume_game_button"),
        ) {
          Icon(Icons.Filled.PlayArrow, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("ПРОДОЛЖИТЬ ИГРУ", style = MaterialTheme.typography.labelLarge)
        }
      }

      Button(
        onClick = onStartNewGame,
        colors = ButtonDefaults.buttonColors(containerColor = SpectrumCyan, contentColor = SpectrumVoid),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("start_game_button"),
      ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("СТАРТ ИГРЫ", style = MaterialTheme.typography.labelLarge)
      }

      // Quick Touch/Audio/CRT toggles
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = uiState.controlMode == TouchControlMode.ARCADE_PAD,
          onClick = onToggleControlMode,
          label = {
            Text(
              text = if (uiState.controlMode == TouchControlMode.ARCADE_PAD) "Кнопки" else "Свайпы",
              style = MaterialTheme.typography.labelSmall,
            )
          },
          leadingIcon = { Icon(Icons.Filled.Gamepad, contentDescription = null) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = SpectrumCyan.copy(alpha = 0.22f),
              selectedLabelColor = SpectrumCyan,
              selectedLeadingIconColor = SpectrumCyan,
              labelColor = SpectrumWhite,
              iconColor = SpectrumWhite,
            ),
          modifier = Modifier.weight(1f).testTag("toggle_control_mode_chip"),
        )

        FilterChip(
          selected = uiState.soundEnabled,
          onClick = onToggleSound,
          label = {
            Text(
              text = if (uiState.soundEnabled) "Звук" else "Тихо",
              style = MaterialTheme.typography.labelSmall,
            )
          },
          leadingIcon = {
            Icon(
              if (uiState.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
              contentDescription = null,
            )
          },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = SpectrumMagenta.copy(alpha = 0.22f),
              selectedLabelColor = SpectrumMagenta,
              selectedLeadingIconColor = SpectrumMagenta,
              labelColor = SpectrumMuted,
              iconColor = SpectrumMuted,
            ),
          modifier = Modifier.weight(1f).testTag("toggle_sound_chip"),
        )

        FilterChip(
          selected = uiState.crtScanlines,
          onClick = onToggleCrt,
          label = {
            Text(
              text = "CRT",
              style = MaterialTheme.typography.labelSmall,
            )
          },
          leadingIcon = { Icon(Icons.Filled.Tv, contentDescription = null) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = SpectrumYellow.copy(alpha = 0.22f),
              selectedLabelColor = SpectrumYellow,
              selectedLeadingIconColor = SpectrumYellow,
              labelColor = SpectrumMuted,
              iconColor = SpectrumMuted,
            ),
          modifier = Modifier.weight(0.85f).testTag("toggle_crt_chip"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = { showScores = true },
          modifier = Modifier.weight(1f).height(48.dp).testTag("open_scores_button"),
        ) {
          Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = SpectrumYellow)
          Spacer(Modifier.width(6.dp))
          Text("РЕКОРДЫ", style = MaterialTheme.typography.labelSmall, color = SpectrumWhite)
        }

        OutlinedButton(
          onClick = { showGuide = true },
          modifier = Modifier.weight(1f).height(48.dp).testTag("open_guide_button"),
        ) {
          Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = SpectrumCyan)
          Spacer(Modifier.width(6.dp))
          Text("ПРАВИЛА", style = MaterialTheme.typography.labelSmall, color = SpectrumWhite)
        }
      }
    }
  }

  if (showScores) {
    HighScoresDialog(scores = uiState.highScores, onDismiss = { showScores = false })
  }
  if (showGuide) {
    ControlsGuideDialog(onDismiss = { showGuide = false })
  }
}
