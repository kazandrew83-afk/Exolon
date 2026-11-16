package com.example.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.game.model.HighScoreEntry
import com.example.ui.theme.SpectrumBorder
import com.example.ui.theme.SpectrumCyan
import com.example.ui.theme.SpectrumGreen
import com.example.ui.theme.SpectrumMagenta
import com.example.ui.theme.SpectrumMuted
import com.example.ui.theme.SpectrumSurface
import com.example.ui.theme.SpectrumSurfaceElevated
import com.example.ui.theme.SpectrumVoid
import com.example.ui.theme.SpectrumWhite
import com.example.ui.theme.SpectrumYellow

@Composable
fun HighScoresDialog(
  scores: List<HighScoreEntry>,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SpectrumSurface,
      modifier = Modifier.fillMaxWidth().border(2.dp, SpectrumCyan, RoundedCornerShape(16.dp)),
    ) {
      Column(
        modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "ТАБЛИЦА РЕКОРДОВ",
          style = MaterialTheme.typography.headlineMedium,
          color = SpectrumYellow,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "ЗАЛ СЛАВЫ EXOLON",
          style = MaterialTheme.typography.labelSmall,
          color = SpectrumMuted,
        )
        Spacer(modifier = Modifier.height(14.dp))

        scores.take(10).forEachIndexed { idx, entry ->
          val rowColor =
            when {
              entry.isPlayer -> SpectrumCyan
              idx == 0 -> SpectrumYellow
              idx < 3 -> SpectrumMagenta
              else -> SpectrumWhite
            }
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(
                  if (entry.isPlayer) SpectrumSurfaceElevated else SpectrumVoid.copy(alpha = 0.55f),
                  RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "${idx + 1}.",
                style = MaterialTheme.typography.labelLarge,
                color = SpectrumMuted,
                modifier = Modifier.width(28.dp),
              )
              Text(
                text = entry.name,
                style = MaterialTheme.typography.labelLarge,
                color = rowColor,
              )
            }
            Text(
              text = entry.score.toString().padStart(6, '0'),
              style = MaterialTheme.typography.labelLarge,
              color = rowColor,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = SpectrumCyan, contentColor = SpectrumVoid),
          modifier = Modifier.fillMaxWidth().testTag("close_scores_button"),
        ) {
          Text("ЗАКРЫТЬ", style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

@Composable
fun ControlsGuideDialog(onDismiss: () -> Unit) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SpectrumSurface,
      modifier = Modifier.fillMaxWidth().border(2.dp, SpectrumMagenta, RoundedCornerShape(16.dp)),
    ) {
      Column(
        modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "КАК ИГРАТЬ",
          style = MaterialTheme.typography.headlineMedium,
          color = SpectrumCyan,
        )

        GuideItem(
          title = "ПРОСТОЕ ТАЧ-УПРАВЛЕНИЕ",
          desc =
            "Слева: ведите палец между ВЛЕВО / ВПРАВО / ПРИСЕСТЬ. Справа: ПРЫЖОК, ОГОНЬ (лазер) и отдельная кнопка ГРАНАТА (или зажмите ОГОНЬ). В меню можно выбрать режим Свайпов.",
          accent = SpectrumCyan,
        )
        GuideItem(
          title = "ЭКЗО-КОСТЮМ (EXOLON)",
          desc =
            "Зайдите в жёлто-голубую капсулу — получите двойной лазер, усиленный щит (+1 попадание) и пополнение боезапаса.",
          accent = SpectrumYellow,
        )
        GuideItem(
          title = "ГРАНАТЫ И УКРЕПЛЕНИЯ",
          desc =
            "Фиолетовые стены-бункеры, мины на полу и тяжёлые пушки проще всего уничтожаются гранатами. Зажмите ПРИСЕСТЬ, чтобы проскочить под выстрелами турелей!",
          accent = SpectrumMagenta,
        )
        GuideItem(
          title = "ТЕЛЕПОРТ В КОНЦЕ СЕКТОРА",
          desc =
            "Пройдите 6 экранов и войдите в пурпурный телепорт на 6-м экране, чтобы перейти в следующий сектор (+1000 очков).",
          accent = SpectrumGreen,
        )

        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = SpectrumMagenta, contentColor = SpectrumVoid),
          modifier = Modifier.fillMaxWidth().testTag("close_guide_button"),
        ) {
          Text("ПОНЯТНО!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun GuideItem(
  title: String,
  desc: String,
  accent: androidx.compose.ui.graphics.Color,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(SpectrumSurfaceElevated, RoundedCornerShape(10.dp))
        .border(1.dp, SpectrumBorder, RoundedCornerShape(10.dp))
        .padding(10.dp)
  ) {
    Text(text = title, style = MaterialTheme.typography.labelLarge, color = accent)
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = SpectrumWhite.copy(alpha = 0.9f))
  }
}
