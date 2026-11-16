package com.example.game.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.TouchControlMode
import com.example.game.model.TouchInputState
import com.example.ui.theme.SpectrumBorder
import com.example.ui.theme.SpectrumCyan
import com.example.ui.theme.SpectrumMagenta
import com.example.ui.theme.SpectrumMuted
import com.example.ui.theme.SpectrumRed
import com.example.ui.theme.SpectrumSurface
import com.example.ui.theme.SpectrumSurfaceElevated
import com.example.ui.theme.SpectrumWhite
import com.example.ui.theme.SpectrumYellow
import kotlin.math.abs

@Composable
fun ExolonTouchController(
  mode: TouchControlMode,
  touchInput: TouchInputState,
  grenadeCount: Int,
  compactOverlay: Boolean,
  modifier: Modifier = Modifier,
) {
  when (mode) {
    TouchControlMode.ARCADE_PAD ->
      ArcadePadControls(
        touchInput = touchInput,
        grenadeCount = grenadeCount,
        compactOverlay = compactOverlay,
        modifier = modifier,
      )
    TouchControlMode.SWIPE_TAP ->
      SwipeTapControls(
        touchInput = touchInput,
        grenadeCount = grenadeCount,
        compactOverlay = compactOverlay,
        modifier = modifier,
      )
  }
}

@Composable
private fun ArcadePadControls(
  touchInput: TouchInputState,
  grenadeCount: Int,
  compactOverlay: Boolean,
  modifier: Modifier = Modifier,
) {
  val bgAlpha = if (compactOverlay) 0.55f else 0.92f
  val padButtonSize = if (compactOverlay) 54.dp else 62.dp

  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Left Thumb: Continuous Multi-Zone D-Pad Rocker (Left / Duck / Right)
    MovementRockerPad(
      touchInput = touchInput,
      buttonSize = padButtonSize,
      bgAlpha = bgAlpha,
    )

    // Right Thumb: Jump + Fire + Grenade (independent multi-touch buttons)
    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ArcadeHoldButton(
          label = "ГРАН ($grenadeCount)",
          icon = Icons.Filled.Brightness7,
          accentColor = SpectrumYellow,
          size = if (compactOverlay) 54.dp else 62.dp,
          bgAlpha = bgAlpha,
          testTag = "control_grenade_button",
          contentDesc = "Бросить гранату",
          enabledVisual = grenadeCount > 0,
          onPressedChange = { pressed -> touchInput.grenade = pressed },
        )

        ArcadeHoldButton(
          label = "ПРЫЖОК",
          icon = Icons.Filled.ArrowUpward,
          accentColor = SpectrumCyan,
          size = if (compactOverlay) 60.dp else 68.dp,
          bgAlpha = bgAlpha,
          testTag = "control_jump_button",
          contentDesc = "Прыжок",
          onPressedChange = { pressed -> touchInput.jump = pressed },
        )
      }

      ArcadeHoldButton(
        label = "ОГОНЬ",
        icon = Icons.Filled.Bolt,
        accentColor = SpectrumMagenta,
        width = if (compactOverlay) 128.dp else 144.dp,
        height = if (compactOverlay) 54.dp else 60.dp,
        bgAlpha = bgAlpha,
        testTag = "control_fire_button",
        contentDesc = "Стрелять лазером",
        onPressedChange = { pressed -> touchInput.fire = pressed },
      )
    }
  }
}

@Composable
private fun MovementRockerPad(
  touchInput: TouchInputState,
  buttonSize: Dp,
  bgAlpha: Float,
) {
  var leftActive by remember { mutableStateOf(false) }
  var rightActive by remember { mutableStateOf(false) }
  var duckActive by remember { mutableStateOf(false) }
  var padSize by remember { mutableStateOf(IntSize.Zero) }
  val haptics = LocalHapticFeedback.current

  fun updateFromPointers(positions: List<Pair<Float, Float>>) {
    if (padSize.width == 0 || padSize.height == 0 || positions.isEmpty()) {
      leftActive = false
      rightActive = false
      duckActive = false
      touchInput.left = false
      touchInput.right = false
      touchInput.duck = false
      return
    }
    var l = false
    var r = false
    var d = false
    val w = padSize.width.toFloat()
    val h = padSize.height.toFloat()
    for ((px, py) in positions) {
      val nx = px / w
      val ny = py / h
      if (nx < 0.38f) l = true
      else if (nx > 0.62f) r = true
      if (ny > 0.58f && nx in 0.25f..0.75f) d = true
    }
    if ((l && !leftActive) || (r && !rightActive) || (d && !duckActive)) {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    leftActive = l
    rightActive = r
    duckActive = d
    touchInput.left = l
    touchInput.right = r
    touchInput.duck = d
  }

  Column(
    modifier =
      Modifier
        .testTag("control_dpad_rocker")
        .semantics { contentDescription = "Блок перемещения: влево, вниз присесть, вправо" }
        .onSizeChanged { padSize = it }
        .pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              val activePositions =
                event.changes
                  .filter { it.pressed }
                  .map { it.position.x to it.position.y }
              updateFromPointers(activePositions)
              event.changes.forEach { it.consume() }
            }
          }
        },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      DirectionVisualTile(
        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        label = "ВЛЕВО",
        active = leftActive,
        accent = SpectrumCyan,
        size = buttonSize,
        bgAlpha = bgAlpha,
      )
      DirectionVisualTile(
        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        label = "ВПРАВО",
        active = rightActive,
        accent = SpectrumCyan,
        size = buttonSize,
        bgAlpha = bgAlpha,
      )
    }
    DirectionVisualTile(
      icon = Icons.Filled.KeyboardArrowDown,
      label = "ПРИСЕСТЬ",
      active = duckActive,
      accent = SpectrumYellow,
      width = buttonSize * 2 + 10.dp,
      height = 48.dp,
      bgAlpha = bgAlpha,
    )
  }
}

@Composable
private fun DirectionVisualTile(
  icon: ImageVector,
  label: String,
  active: Boolean,
  accent: Color,
  size: Dp = 60.dp,
  width: Dp = size,
  height: Dp = size,
  bgAlpha: Float,
) {
  val bg = if (active) accent.copy(alpha = 0.30f) else SpectrumSurfaceElevated.copy(alpha = bgAlpha)
  val border = if (active) accent else SpectrumBorder
  Box(
    modifier =
      Modifier
        .size(width = width, height = height)
        .clip(RoundedCornerShape(14.dp))
        .background(bg)
        .border(width = if (active) 2.dp else 1.5.dp, color = border, shape = RoundedCornerShape(14.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (active) SpectrumWhite else accent,
        modifier = Modifier.size(26.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
        color = if (active) SpectrumWhite else SpectrumMuted,
      )
    }
  }
}

@Composable
private fun ArcadeHoldButton(
  label: String,
  icon: ImageVector,
  accentColor: Color,
  size: Dp = 64.dp,
  width: Dp = size,
  height: Dp = size,
  bgAlpha: Float,
  testTag: String,
  contentDesc: String,
  enabledVisual: Boolean = true,
  onPressedChange: (Boolean) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  val haptics = LocalHapticFeedback.current
  val effectiveAccent = if (enabledVisual) accentColor else SpectrumMuted
  val shape = if (width == height) CircleShape else RoundedCornerShape(18.dp)

  Box(
    modifier =
      Modifier
        .size(width = width, height = height)
        .testTag(testTag)
        .semantics { contentDescription = contentDesc }
        .clip(shape)
        .background(
          if (pressed) effectiveAccent.copy(alpha = 0.36f)
          else SpectrumSurfaceElevated.copy(alpha = bgAlpha)
        )
        .border(
          width = if (pressed) 2.5.dp else 1.5.dp,
          color = if (pressed) SpectrumWhite else effectiveAccent.copy(alpha = 0.85f),
          shape = shape,
        )
        .pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              val isDown = event.changes.any { it.pressed }
              if (isDown != pressed) {
                pressed = isDown
                if (isDown) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPressedChange(isDown)
              }
              if (event.type == PointerEventType.Press || event.type == PointerEventType.Release || isDown) {
                event.changes.forEach { it.consume() }
              }
            }
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (pressed) SpectrumWhite else effectiveAccent,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
        color = if (pressed) SpectrumWhite else SpectrumWhite.copy(alpha = 0.9f),
      )
    }
  }
}

@Composable
private fun SwipeTapControls(
  touchInput: TouchInputState,
  grenadeCount: Int,
  compactOverlay: Boolean,
  modifier: Modifier = Modifier,
) {
  val bgAlpha = if (compactOverlay) 0.42f else 0.78f
  var leftGestureHint by remember { mutableStateOf("СВАЙП: ХОД / ПРЫЖОК / ПРИСЕД") }
  var rightGestureHint by remember { mutableStateOf("ТАП: ЛАЗЕР • СВАЙП ВВЕРХ: ГРАНАТА ($grenadeCount)") }

  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Left Swipe Pad
    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(16.dp))
          .background(SpectrumSurface.copy(alpha = bgAlpha))
          .border(1.5.dp, SpectrumCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
          .testTag("swipe_move_zone")
          .semantics { contentDescription = "Зона свайпов движения и прыжка" }
          .pointerInput(Unit) {
            awaitPointerEventScope {
              var originX = 0f
              var originY = 0f
              var trackingId: Long? = null
              while (true) {
                val event = awaitPointerEvent()
                val active = event.changes.firstOrNull { it.pressed }
                if (active == null) {
                  trackingId = null
                  touchInput.left = false
                  touchInput.right = false
                  touchInput.duck = false
                  touchInput.jump = false
                  leftGestureHint = "ВЕДИТЕ ПАЛЬЦЕМ ДЛЯ ХОДЬБЫ"
                } else {
                  if (trackingId != active.id.value) {
                    trackingId = active.id.value
                    originX = active.position.x
                    originY = active.position.y
                  }
                  val dx = active.position.x - originX
                  val dy = active.position.y - originY
                  touchInput.left = dx < -18f
                  touchInput.right = dx > 18f
                  touchInput.duck = dy > 28f && abs(dy) > abs(dx) * 0.7f
                  touchInput.jump = dy < -28f
                  leftGestureHint =
                    when {
                      touchInput.jump -> "ПРЫЖОК ▲"
                      touchInput.duck -> "ПРИСЕД ▼"
                      touchInput.left -> "◀ ВЛЕВО"
                      touchInput.right -> "ВПРАВО ▶"
                      else -> "ВЕДИТЕ ПАЛЬЦЕМ..."
                    }
                }
                event.changes.forEach { it.consume() }
              }
            }
          },
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
        Text(
          text = "ДВИЖЕНИЕ",
          style = MaterialTheme.typography.labelLarge,
          color = SpectrumCyan,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = leftGestureHint,
          style = MaterialTheme.typography.bodyMedium,
          color = SpectrumWhite.copy(alpha = 0.85f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "◀ ▶ Ходьба  •  ▲ Прыжок  •  ▼ Присед",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
          color = SpectrumMuted,
        )
      }
    }

    // Right Tap/Swipe Attack Pad
    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(16.dp))
          .background(SpectrumSurface.copy(alpha = bgAlpha))
          .border(1.5.dp, SpectrumMagenta.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
          .testTag("swipe_fire_zone")
          .semantics { contentDescription = "Зона стрельбы лазером и броска гранаты" }
          .pointerInput(Unit) {
            awaitPointerEventScope {
              var originY = 0f
              var trackingId: Long? = null
              while (true) {
                val event = awaitPointerEvent()
                val active = event.changes.firstOrNull { it.pressed }
                if (active == null) {
                  trackingId = null
                  touchInput.fire = false
                  touchInput.grenade = false
                  rightGestureHint = "ТАП: ЛАЗЕР • ВВЕРХ: ГРАНАТА ($grenadeCount)"
                } else {
                  if (trackingId != active.id.value) {
                    trackingId = active.id.value
                    originY = active.position.y
                  }
                  val dy = active.position.y - originY
                  if (dy < -28f) {
                    touchInput.grenade = true
                    touchInput.fire = false
                    rightGestureHint = "ГРАНАТА! ▲"
                  } else {
                    touchInput.fire = true
                    touchInput.grenade = false
                    rightGestureHint = "ОГОНЬ ЛАЗЕРОМ!"
                  }
                }
                event.changes.forEach { it.consume() }
              }
            }
          },
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
        Text(
          text = "АТАКА",
          style = MaterialTheme.typography.labelLarge,
          color = SpectrumMagenta,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = rightGestureHint,
          style = MaterialTheme.typography.bodyMedium,
          color = SpectrumWhite.copy(alpha = 0.85f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Удерживайте: Огонь • Свайп ▲: Граната",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
          color = SpectrumMuted,
        )
      }
    }
  }
}
