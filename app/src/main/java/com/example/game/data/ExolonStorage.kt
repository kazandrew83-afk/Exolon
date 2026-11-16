package com.example.game.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.model.HighScoreEntry
import com.example.game.model.TouchControlMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.exolonDataStore by preferencesDataStore(name = "exolon_prefs")

data class ExolonPrefsSnapshot(
  val soundEnabled: Boolean = true,
  val crtScanlines: Boolean = true,
  val controlMode: TouchControlMode = TouchControlMode.ARCADE_PAD,
  val highScores: List<HighScoreEntry> = DEFAULT_SPECTRUM_SCORES,
)

val DEFAULT_SPECTRUM_SCORES =
  listOf(
    HighScoreEntry("RAFFAELE", 2400),
    HighScoreEntry("SURYANI", 2000),
    HighScoreEntry("SCRAGGY", 1750),
    HighScoreEntry("SABATTA!", 1500),
    HighScoreEntry("PADDY", 1250),
    HighScoreEntry("LOFTY", 1000),
    HighScoreEntry("JIMBO", 800),
    HighScoreEntry("FIGARO", 600),
  )

class ExolonStorage(private val context: Context) {

  private object Keys {
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val CRT_SCANLINES = booleanPreferencesKey("crt_scanlines")
    val CONTROL_MODE = stringPreferencesKey("control_mode")
    val HIGH_SCORES = stringPreferencesKey("high_scores_v1")
    val BEST_SCORE = intPreferencesKey("best_score")
  }

  val prefsFlow: Flow<ExolonPrefsSnapshot> =
    context.exolonDataStore.data.map { prefs ->
      val modeName = prefs[Keys.CONTROL_MODE] ?: TouchControlMode.ARCADE_PAD.name
      val controlMode =
        runCatching { TouchControlMode.valueOf(modeName) }.getOrDefault(TouchControlMode.ARCADE_PAD)
      val scoresRaw = prefs[Keys.HIGH_SCORES]
      val scores =
        if (scoresRaw.isNullOrBlank()) {
          DEFAULT_SPECTRUM_SCORES
        } else {
          decodeScores(scoresRaw).ifEmpty { DEFAULT_SPECTRUM_SCORES }
        }
      ExolonPrefsSnapshot(
        soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
        crtScanlines = prefs[Keys.CRT_SCANLINES] ?: true,
        controlMode = controlMode,
        highScores = scores.sortedByDescending { it.score }.take(10),
      )
    }

  suspend fun setSoundEnabled(enabled: Boolean) {
    context.exolonDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
  }

  suspend fun setCrtScanlines(enabled: Boolean) {
    context.exolonDataStore.edit { it[Keys.CRT_SCANLINES] = enabled }
  }

  suspend fun setControlMode(mode: TouchControlMode) {
    context.exolonDataStore.edit { it[Keys.CONTROL_MODE] = mode.name }
  }

  suspend fun recordScore(playerName: String, score: Int): List<HighScoreEntry> {
    var updated = DEFAULT_SPECTRUM_SCORES
    context.exolonDataStore.edit { prefs ->
      val existing =
        prefs[Keys.HIGH_SCORES]?.let { decodeScores(it) }?.ifEmpty { DEFAULT_SPECTRUM_SCORES }
          ?: DEFAULT_SPECTRUM_SCORES
      val cleanName = playerName.trim().uppercase().take(10).ifEmpty { "PLAYER" }
      updated =
        (existing + HighScoreEntry(name = cleanName, score = score, isPlayer = true))
          .sortedByDescending { it.score }
          .take(10)
      prefs[Keys.HIGH_SCORES] = encodeScores(updated)
      val top = updated.firstOrNull()?.score ?: score
      prefs[Keys.BEST_SCORE] = top
    }
    return updated
  }

  private fun encodeScores(entries: List<HighScoreEntry>): String =
    entries.joinToString("|") { "${it.name.replace("|", "").replace(":", "")}:${it.score}:${if (it.isPlayer) 1 else 0}" }

  private fun decodeScores(raw: String): List<HighScoreEntry> =
    raw.split("|").mapNotNull { token ->
      val parts = token.split(":")
      if (parts.size >= 2) {
        val name = parts[0]
        val score = parts[1].toIntOrNull() ?: return@mapNotNull null
        val isPlayer = parts.getOrNull(2) == "1"
        HighScoreEntry(name = name, score = score, isPlayer = isPlayer)
      } else {
        null
      }
    }
}
