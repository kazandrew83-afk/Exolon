package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.model.ScreenPhase
import com.example.game.ui.screens.GameScreen
import com.example.game.ui.screens.TitleScreen
import com.example.game.viewmodel.ExolonViewModel
import com.example.ui.theme.ExolonTheme

class MainActivity : ComponentActivity() {

  private val gameViewModel: ExolonViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    setContent {
      ExolonTheme {
        val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()

        if (uiState.phase == ScreenPhase.TITLE) {
          TitleScreen(
            uiState = uiState,
            onStartNewGame = gameViewModel::startNewGame,
            onResumeGame = gameViewModel::resumeGame,
            onToggleSound = gameViewModel::toggleSound,
            onToggleCrt = gameViewModel::toggleCrt,
            onToggleControlMode = gameViewModel::toggleControlMode,
          )
        } else {
          GameScreen(
            uiState = uiState,
            touchInput = gameViewModel.touchInput,
            onPause = gameViewModel::pauseGame,
            onResume = gameViewModel::resumeGame,
            onRestart = gameViewModel::startNewGame,
            onExitToMenu = gameViewModel::returnToTitle,
            onToggleSound = gameViewModel::toggleSound,
            onToggleCrt = gameViewModel::toggleCrt,
            onToggleControlMode = gameViewModel::toggleControlMode,
            onSaveScore = gameViewModel::submitScoreName,
          )
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    gameViewModel.pauseGame()
  }
}
