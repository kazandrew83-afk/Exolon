package com.example.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.ExolonLevels
import com.example.game.model.GameUiState
import com.example.game.model.ScreenPhase
import com.example.game.model.TouchControlMode
import com.example.game.model.TouchInputState
import com.example.game.ui.canvas.ExolonGameCanvas
import com.example.game.ui.controls.ExolonTouchController
import com.example.ui.theme.SpectrumBorder
import com.example.ui.theme.SpectrumCyan
import com.example.ui.theme.SpectrumGreen
import com.example.ui.theme.SpectrumMagenta
import com.example.ui.theme.SpectrumMuted
import com.example.ui.theme.SpectrumRed
import com.example.ui.theme.SpectrumSurface
import com.example.ui.theme.SpectrumSurfaceElevated
import com.example.ui.theme.SpectrumVoid
import com.example.ui.theme.SpectrumWhite
import com.example.ui.theme.SpectrumYellow

@Composable
fun GameScreen(
  uiState: GameUiState,
  touchInput: TouchInputState,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onExitToMenu: () -> Unit,
  onToggleSound: () -> Unit,
  onToggleCrt: () -> Unit,
  onToggleControlMode: () -> Unit,
  onSaveScore: (String) -> Unit,
) {
  BackHandler {
    when (uiState.phase) {
      ScreenPhase.PLAYING -> onPause()
      ScreenPhase.PAUSED -> onResume()
      else -> onExitToMenu()
    }
  }

  BoxWithConstraints(
    modifier =
      Modifier
        .fillMaxSize()
        .background(SpectrumVoid)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    val isLandscape = maxWidth > maxHeight * 1.15f

    if (isLandscape) {
      // Landscape: Compact HUD at top + full-bleed canvas + semi-transparent ergonomic touch controls
      Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
          GameHudBar(
            uiState = uiState,
            onPause = onPause,
            onToggleControlMode = onToggleControlMode,
          )
          Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
          ) {
            ExolonGameCanvas(
              uiState = uiState,
              modifier = Modifier.fillMaxHeight(),
            )
            BannerPopup(uiState.bannerText)
          }
        }

        if (uiState.phase == ScreenPhase.PLAYING) {
          ExolonTouchController(
            mode = uiState.controlMode,
            touchInput = touchInput,
            grenadeCount = uiState.grenades,
            compactOverlay = true,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
          )
        }
      }
    } else {
      // Portrait: Top HUD + Crisp 16:10 Playfield + Dedicated Spacious Arcade Touch Deck below
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        GameHudBar(
          uiState = uiState,
          onPause = onPause,
          onToggleControlMode = onToggleControlMode,
        )

        Spacer(Modifier.height(4.dp))

        Box(
          modifier = Modifier.fillMaxWidth().weight(0.52f),
          contentAlignment = Alignment.Center,
        ) {
          ExolonGameCanvas(
            uiState = uiState,
            modifier = Modifier.fillMaxWidth(),
          )
          BannerPopup(uiState.bannerText)
        }

        // Bottom Tactile Touch Controller Deck
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .weight(0.44f)
              .padding(top = 6.dp, bottom = 4.dp)
              .background(SpectrumSurface.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
              .border(1.dp, SpectrumBorder, RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center,
        ) {
          ExolonTouchController(
            mode = uiState.controlMode,
            touchInput = touchInput,
            grenadeCount = uiState.grenades,
            compactOverlay = false,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }

    // Overlays: Pause, Game Over, Victory
    when (uiState.phase) {
      ScreenPhase.PAUSED ->
        PauseOverlay(
          uiState = uiState,
          onResume = onResume,
          onRestart = onRestart,
          onExitToMenu = onExitToMenu,
          onToggleSound = onToggleSound,
          onToggleCrt = onToggleCrt,
          onToggleControlMode = onToggleControlMode,
        )
      ScreenPhase.GAME_OVER ->
        EndGameOverlay(
          isVictory = false,
          score = uiState.score,
          highScore = uiState.highScore,
          onSaveScore = onSaveScore,
          onRestart = onRestart,
          onExitToMenu = onExitToMenu,
        )
      ScreenPhase.VICTORY ->
        EndGameOverlay(
          isVictory = true,
          score = uiState.score,
          highScore = uiState.highScore,
          onSaveScore = onSaveScore,
          onRestart = onRestart,
          onExitToMenu = onExitToMenu,
        )
      else -> Unit
    }
  }
}

@Composable
private fun GameHudBar(
  uiState: GameUiState,
  onPause: () -> Unit,
  onToggleControlMode: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(SpectrumSurface, RoundedCornerShape(10.dp))
        .border(1.dp, SpectrumBorder, RoundedCornerShape(10.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HudStat(
        label = "СЧЁТ",
        value = uiState.score.toString().padStart(6, '0'),
        color = SpectrumCyan,
      )
      HudStat(
        label = "СЕКТОР",
        value = "${uiState.zoneIndex + 1}-${uiState.screenIndex + 1}/${ExolonLevels.SCREENS_PER_ZONE}",
        color = SpectrumYellow,
      )
      HudStat(
        label = "ЛАЗЕР",
        value = uiState.ammo.toString().padStart(2, '0'),
        color = if (uiState.ammo <= 10) SpectrumRed else SpectrumWhite,
      )
      HudStat(
        label = "ГРАН",
        value = uiState.grenades.toString().padStart(2, '0'),
        color = SpectrumGreen,
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onToggleControlMode,
          modifier = Modifier.size(48.dp).testTag("hud_switch_controls_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Gamepad,
            contentDescription = "Переключить режим управления",
            tint = if (uiState.controlMode == TouchControlMode.ARCADE_PAD) SpectrumCyan else SpectrumMagenta,
          )
        }
        IconButton(
          onClick = onPause,
          modifier = Modifier.size(48.dp).testTag("hud_pause_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Pause,
            contentDescription = "Пауза",
            tint = SpectrumWhite,
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = uiState.zoneName,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = SpectrumMuted,
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (uiState.player.isExoSuit) {
          Text(
            text = "EXO-ARMOR",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SpectrumVoid,
            modifier =
              Modifier
                .background(SpectrumYellow, RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp),
          )
          Spacer(Modifier.width(6.dp))
        }
        repeat(uiState.lives.coerceAtMost(6)) {
          Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = SpectrumRed,
            modifier = Modifier.size(13.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun HudStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
      color = SpectrumMuted,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
      color = color,
    )
  }
}

@Composable
private fun BannerPopup(text: String?) {
  AnimatedVisibility(
    visible = !text.isNullOrBlank(),
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    Box(
      modifier =
        Modifier
          .background(SpectrumVoid.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
          .border(1.5.dp, SpectrumYellow, RoundedCornerShape(8.dp))
          .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
      Text(
        text = text.orEmpty(),
        style = MaterialTheme.typography.labelLarge,
        color = SpectrumYellow,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun PauseOverlay(
  uiState: GameUiState,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onExitToMenu: () -> Unit,
  onToggleSound: () -> Unit,
  onToggleCrt: () -> Unit,
  onToggleControlMode: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().background(SpectrumVoid.copy(alpha = 0.82f)).padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SpectrumSurface,
      modifier = Modifier.fillMaxWidth(0.92f).border(2.dp, SpectrumCyan, RoundedCornerShape(16.dp)),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("ПАУЗА", style = MaterialTheme.typography.displaySmall, color = SpectrumCyan)

        Button(
          onClick = onResume,
          colors = ButtonDefaults.buttonColors(containerColor = SpectrumCyan, contentColor = SpectrumVoid),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pause_resume_button"),
        ) {
          Text("ПРОДОЛЖИТЬ", style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
          onClick = onToggleControlMode,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_controls_button"),
        ) {
          Icon(Icons.Filled.Gamepad, contentDescription = null, tint = SpectrumYellow)
          Spacer(Modifier.width(8.dp))
          Text(
            text =
              if (uiState.controlMode == TouchControlMode.ARCADE_PAD) "Управление: Кнопки"
              else "Управление: Свайпы",
            style = MaterialTheme.typography.labelLarge,
            color = SpectrumWhite,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onToggleSound,
            modifier = Modifier.weight(1f).height(48.dp).testTag("pause_sound_button"),
          ) {
            Icon(
              if (uiState.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
              contentDescription = null,
              tint = SpectrumMagenta,
            )
            Spacer(Modifier.width(4.dp))
            Text(
              if (uiState.soundEnabled) "ЗВУК" else "ТИХО",
              style = MaterialTheme.typography.labelSmall,
              color = SpectrumWhite,
            )
          }

          OutlinedButton(
            onClick = onToggleCrt,
            modifier = Modifier.weight(1f).height(48.dp).testTag("pause_crt_button"),
          ) {
            Text(
              if (uiState.crtScanlines) "CRT: ВКЛ" else "CRT: ВЫКЛ",
              style = MaterialTheme.typography.labelSmall,
              color = SpectrumWhite,
            )
          }
        }

        OutlinedButton(
          onClick = onRestart,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_restart_button"),
        ) {
          Text("НАЧАТЬ ЗАНОВО", style = MaterialTheme.typography.labelLarge, color = SpectrumYellow)
        }

        OutlinedButton(
          onClick = onExitToMenu,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_menu_button"),
        ) {
          Text("В ГЛАВНОЕ МЕНЮ", style = MaterialTheme.typography.labelLarge, color = SpectrumMuted)
        }
      }
    }
  }
}

@Composable
private fun EndGameOverlay(
  isVictory: Boolean,
  score: Int,
  highScore: Int,
  onSaveScore: (String) -> Unit,
  onRestart: () -> Unit,
  onExitToMenu: () -> Unit,
) {
  var playerName by remember { mutableStateOf("COMMANDER") }
  var scoreSaved by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier.fillMaxSize().background(SpectrumVoid.copy(alpha = 0.86f)).padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SpectrumSurface,
      modifier =
        Modifier
          .fillMaxWidth(0.92f)
          .border(2.dp, if (isVictory) SpectrumGreen else SpectrumRed, RoundedCornerShape(16.dp)),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = if (isVictory) "ПОБЕДА!" else "ИГРА ОКОНЧЕНА",
          style = MaterialTheme.typography.displaySmall,
          color = if (isVictory) SpectrumGreen else SpectrumRed,
          textAlign = TextAlign.Center,
        )
        Text(
          text =
            if (isVictory) "Все 5 секторов базы Exolon зачищены!"
            else "Вы пали в бою за планету Exolon",
          style = MaterialTheme.typography.bodyMedium,
          color = SpectrumMuted,
          textAlign = TextAlign.Center,
        )

        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(SpectrumSurfaceElevated, RoundedCornerShape(10.dp))
              .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceAround,
        ) {
          HudStat("ВАШ СЧЁТ", score.toString().padStart(6, '0'), SpectrumCyan)
          HudStat("РЕКОРД", highScore.toString().padStart(6, '0'), SpectrumYellow)
        }

        if (score > 0 && !scoreSaved) {
          OutlinedTextField(
            value = playerName,
            onValueChange = { if (it.length <= 10) playerName = it },
            label = { Text("ИМЯ ДЛЯ РЕКОРДОВ") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("score_name_input"),
          )
          Button(
            onClick = {
              onSaveScore(playerName)
              scoreSaved = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = SpectrumYellow, contentColor = SpectrumVoid),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_score_button"),
          ) {
            Text("СОХРАНИТЬ РЕКОРД", style = MaterialTheme.typography.labelLarge)
          }
        } else if (scoreSaved) {
          Text(
            text = "РЕКОРД СОХРАНЁН!",
            style = MaterialTheme.typography.labelLarge,
            color = SpectrumGreen,
          )
        }

        Button(
          onClick = onRestart,
          colors = ButtonDefaults.buttonColors(containerColor = SpectrumCyan, contentColor = SpectrumVoid),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("end_restart_button"),
        ) {
          Text("ИГРАТЬ СНОВА", style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
          onClick = onExitToMenu,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("end_menu_button"),
        ) {
          Text("ГЛАВНОЕ МЕНЮ", style = MaterialTheme.typography.labelLarge, color = SpectrumWhite)
        }
      }
    }
  }
}
