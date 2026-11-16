package com.example.game.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.game.data.ExolonLevels
import com.example.game.model.DroneKind
import com.example.game.model.GAME_HEIGHT
import com.example.game.model.GAME_WIDTH
import com.example.game.model.GameUiState
import com.example.game.model.HazardType
import com.example.game.model.PickupKind
import com.example.game.model.TILE_SIZE
import com.example.game.model.TileType
import com.example.game.ui.sprites.PixelSprites
import com.example.game.ui.sprites.drawPixelSprite
import com.example.ui.theme.SpectrumBlue
import com.example.ui.theme.SpectrumBorder
import com.example.ui.theme.SpectrumCyan
import com.example.ui.theme.SpectrumGreen
import com.example.ui.theme.SpectrumMagenta
import com.example.ui.theme.SpectrumOrange
import com.example.ui.theme.SpectrumRed
import com.example.ui.theme.SpectrumSurface
import com.example.ui.theme.SpectrumVoid
import com.example.ui.theme.SpectrumWhite
import com.example.ui.theme.SpectrumYellow
import kotlin.math.sin

private data class StarDot(val x: Float, val y: Float, val size: Float, val colorIdx: Int, val phase: Float)

@Composable
fun ExolonGameCanvas(
  uiState: GameUiState,
  modifier: Modifier = Modifier,
) {
  val stars = remember {
    List(42) { i ->
      StarDot(
        x = ((i * 53 + 17) % 252).toFloat(),
        y = ((i * 37 + 11) % 108).toFloat(),
        size = if (i % 4 == 0) 1.8f else 1.1f,
        colorIdx = i % 3,
        phase = i * 0.7f,
      )
    }
  }

  val theme = ExolonLevels.zoneThemes[uiState.zoneIndex.coerceIn(0, ExolonLevels.zoneThemes.lastIndex)]
  val groundColor = PixelSprites.palette[theme.groundColorIndex]
  val accentColor = PixelSprites.palette[theme.accentColorIndex]

  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(10.dp))
        .background(SpectrumVoid)
        .border(2.dp, SpectrumBorder, RoundedCornerShape(10.dp))
        .padding(2.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      modifier =
        Modifier
          .fillMaxSize()
          .aspectRatio(GAME_WIDTH / GAME_HEIGHT, matchHeightConstraintsFirst = false)
    ) {
      val scaleX = size.width / GAME_WIDTH
      val scaleY = size.height / GAME_HEIGHT
      val shakeX =
        if (uiState.screenShake > 0.01f) {
          sin(uiState.elapsedSeconds * 85f) * uiState.screenShake * 2.5f
        } else 0f
      val shakeY =
        if (uiState.screenShake > 0.01f) {
          sin(uiState.elapsedSeconds * 110f) * uiState.screenShake * 2.5f
        } else 0f

      withTransform({
        scale(scaleX, scaleY, pivot = Offset.Zero)
        translate(shakeX, shakeY)
      }) {
        // 1. Deep Spectrum Sky + Alien Silhouette Skyline
        drawRect(color = SpectrumVoid, topLeft = Offset.Zero, size = Size(GAME_WIDTH, GAME_HEIGHT))
        drawHorizonSkyline(uiState.zoneIndex, uiState.screenIndex, accentColor, uiState.elapsedSeconds, stars)

        // 2. Terrain Tiles
        uiState.tiles.forEach { tile ->
          drawTile(tile.x, tile.y, tile.type, tile.hp, groundColor, accentColor, uiState.elapsedSeconds)
        }

        // 3. World Objects (Pods, Teleporters, Turrets, Mines, Gates, Silos)
        uiState.objects.forEach { obj ->
          if (!obj.active) return@forEach
          when (obj.type) {
            HazardType.MINE -> {
              val flash = ((uiState.elapsedSeconds * 6f).toInt() % 2 == 0)
              drawPixelSprite(
                PixelSprites.mineSprite,
                obj.x + 2f,
                obj.y,
                obj.width,
                obj.height,
                tintOverride = if (flash) null else SpectrumYellow,
              )
            }

            HazardType.TURRET_LEFT -> {
              drawPixelSprite(PixelSprites.turretLeft, obj.x, obj.y, obj.width, obj.height)
            }

            HazardType.HEAVY_CANNON -> {
              drawPixelSprite(PixelSprites.heavyCannon, obj.x, obj.y, obj.width, obj.height)
            }

            HazardType.ROCKET_SILO -> {
              drawPixelSprite(PixelSprites.rocketSilo, obj.x, obj.y, obj.width, obj.height)
            }

            HazardType.LASER_GATE -> {
              // Pulsing vertical energy beam + destructible base emitter
              val beamPulse = 0.55f + 0.45f * sin(uiState.elapsedSeconds * 14f)
              drawRect(
                color = SpectrumMagenta.copy(alpha = 0.35f * beamPulse),
                topLeft = Offset(obj.x + 2f, obj.gateTopY),
                size = Size(8f, obj.y - obj.gateTopY),
              )
              drawRect(
                color = SpectrumCyan.copy(alpha = beamPulse),
                topLeft = Offset(obj.x + 5f, obj.gateTopY),
                size = Size(2f, obj.y - obj.gateTopY),
              )
              // Top cap
              drawRect(
                color = SpectrumYellow,
                topLeft = Offset(obj.x + 1f, obj.gateTopY - 3f),
                size = Size(10f, 3f),
              )
              // Destructible emitter base
              drawRect(
                color = SpectrumRed,
                topLeft = Offset(obj.x, obj.y),
                size = Size(obj.width, obj.height),
              )
              drawRect(
                color = SpectrumWhite,
                topLeft = Offset(obj.x + 3f, obj.y + 3f),
                size = Size(obj.width - 6f, 4f),
              )
            }

            HazardType.EXO_POD -> {
              drawPodChamber(
                x = obj.x,
                y = obj.y,
                w = obj.width,
                h = obj.height,
                primary = SpectrumYellow,
                secondary = SpectrumCyan,
                time = uiState.elapsedSeconds,
                isTeleporter = false,
              )
            }

            HazardType.TELEPORTER -> {
              drawPodChamber(
                x = obj.x,
                y = obj.y,
                w = obj.width,
                h = obj.height,
                primary = SpectrumMagenta,
                secondary = SpectrumCyan,
                time = uiState.elapsedSeconds,
                isTeleporter = true,
              )
            }
          }
        }

        // 4. Pickups
        uiState.pickups.forEach { item ->
          val bob = sin(uiState.elapsedSeconds * 5f + item.bobPhase) * 1.6f
          val sprite =
            when (item.kind) {
              PickupKind.AMMO -> PixelSprites.pickupAmmo
              PickupKind.GRENADE -> PixelSprites.pickupGrenade
              PickupKind.LIFE -> PixelSprites.pickupLife
              PickupKind.CRYSTAL -> PixelSprites.pickupCrystal
            }
          drawPixelSprite(sprite, item.x, item.y + bob, 11f, 11f)
        }

        // 5. Flying Alien Drones
        uiState.drones.forEach { drone ->
          val sprite =
            when (drone.kind) {
              DroneKind.SINE_EYE -> PixelSprites.droneEye
              DroneKind.SWOOP_HORNET -> PixelSprites.droneHornet
              DroneKind.BOUNCE_ORB -> PixelSprites.droneOrb
            }
          drawPixelSprite(sprite, drone.x, drone.y, 14f, 11f)
        }

        // 6. Player Space Marine / Exosuit
        val p = uiState.player
        val blinkHide = p.invulnerableTimer > 0f && ((p.invulnerableTimer * 18f).toInt() % 2 == 0)
        if (!blinkHide) {
          val stride = ((p.animTimer * 10f).toInt() % 2 == 0)
          val sprite =
            when {
              p.isExoSuit && p.isDucking -> PixelSprites.exoDuck
              p.isExoSuit && stride -> PixelSprites.exoWalkA
              p.isExoSuit -> PixelSprites.exoWalkB
              p.isDucking -> PixelSprites.marineDuck
              stride -> PixelSprites.marineWalkA
              else -> PixelSprites.marineWalkB
            }
          val teleportFlash =
            if (p.teleportAnimTimer > 0f && ((p.teleportAnimTimer * 20f).toInt() % 2 == 0)) SpectrumCyan else null
          drawPixelSprite(
            rows = sprite,
            x = p.x,
            y = p.y,
            targetW = p.width,
            targetH = p.height,
            flipX = !p.facingRight,
            tintOverride = teleportFlash,
          )
          // Exo shield shimmer ring
          if (p.isExoSuit && p.exoShieldHits > 0) {
            drawRect(
              color = SpectrumCyan.copy(alpha = 0.25f + 0.15f * sin(uiState.elapsedSeconds * 10f)),
              topLeft = Offset(p.x - 1.5f, p.y - 1.5f),
              size = Size(p.width + 3f, p.height + 3f),
              style = Stroke(width = 1f),
            )
          }
        }

        // 7. Projectiles (Lasers, Twin Lasers, Grenades, Enemy Bolts, Homing Missiles)
        uiState.projectiles.forEach { proj ->
          when {
            proj.isGrenade -> {
              drawCircle(
                color = SpectrumYellow,
                radius = 2.6f,
                center = Offset(proj.x, proj.y),
              )
              drawCircle(
                color = SpectrumRed,
                radius = 1.3f,
                center = Offset(proj.x, proj.y),
              )
            }
            proj.isHoming -> {
              drawRect(
                color = SpectrumOrange,
                topLeft = Offset(proj.x - 3f, proj.y - 2f),
                size = Size(6f, 4f),
              )
              drawRect(
                color = SpectrumWhite,
                topLeft = Offset(proj.x - 1f, proj.y - 1f),
                size = Size(2f, 2f),
              )
            }
            proj.isPlayer -> {
              val boltColor = if (proj.isTwin) SpectrumYellow else SpectrumCyan
              drawRect(
                color = boltColor,
                topLeft = Offset(proj.x - 4f, proj.y - 1f),
                size = Size(9f, 2.2f),
              )
              drawRect(
                color = SpectrumWhite,
                topLeft = Offset(proj.x - 1f, proj.y - 0.5f),
                size = Size(4f, 1.2f),
              )
            }
            else -> {
              drawRect(
                color = SpectrumRed,
                topLeft = Offset(proj.x - 3f, proj.y - 1.5f),
                size = Size(6f, 3f),
              )
              drawRect(
                color = SpectrumYellow,
                topLeft = Offset(proj.x - 1f, proj.y - 0.8f),
                size = Size(2f, 1.6f),
              )
            }
          }
        }

        // 8. Pixel Explosion & Spark Particles
        uiState.particles.forEach { pt ->
          val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
          val color = PixelSprites.palette.getOrElse(pt.colorIndex) { SpectrumYellow }.copy(alpha = alpha)
          drawRect(
            color = color,
            topLeft = Offset(pt.x - pt.size * 0.5f, pt.y - pt.size * 0.5f),
            size = Size(pt.size, pt.size),
          )
        }
      }

      // 9. Optional CRT Scanlines Overlay (drawn in native pixels)
      if (uiState.crtScanlines) {
        var y = 0f
        while (y < size.height) {
          drawLine(
            color = Color.Black.copy(alpha = 0.22f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.2f,
          )
          y += 4f
        }
      }
    }
  }
}

private fun DrawScope.drawHorizonSkyline(
  zoneIndex: Int,
  screenIndex: Int,
  accent: Color,
  elapsed: Float,
  stars: List<StarDot>,
) {
  // Subtle planet / moon in sky
  val planetX = 205f - screenIndex * 18f
  val planetY = 26f
  drawCircle(
    brush =
      Brush.radialGradient(
        colors = listOf(accent.copy(alpha = 0.35f), SpectrumBlue.copy(alpha = 0.08f), Color.Transparent),
        center = Offset(planetX, planetY),
        radius = 28f,
      ),
    radius = 28f,
    center = Offset(planetX, planetY),
  )
  drawCircle(
    color = accent.copy(alpha = 0.25f),
    radius = 12f,
    center = Offset(planetX, planetY),
  )

  // Twinkling Spectrum starfield
  stars.forEach { star ->
    val twinkle = 0.45f + 0.55f * sin(elapsed * 3.2f + star.phase)
    val c =
      when (star.colorIdx) {
        0 -> SpectrumWhite
        1 -> SpectrumCyan
        else -> accent
      }
    drawRect(
      color = c.copy(alpha = twinkle * 0.85f),
      topLeft = Offset(star.x, star.y),
      size = Size(star.size, star.size),
    )
  }

  // Distant alien base silhouettes along horizon
  val baseColor = SpectrumSurface.copy(alpha = 0.9f)
  val horizonY = 128f
  for (i in 0 until 8) {
    val bx = i * 34f + ((zoneIndex * 11 + screenIndex * 7) % 14)
    val bh = 18f + ((i * 13 + zoneIndex * 5 + screenIndex * 9) % 24)
    drawRect(
      color = baseColor,
      topLeft = Offset(bx, horizonY - bh),
      size = Size(24f, bh),
    )
    drawRect(
      color = accent.copy(alpha = 0.18f),
      topLeft = Offset(bx + 4f, horizonY - bh + 4f),
      size = Size(4f, 4f),
    )
  }
}

private fun DrawScope.drawTile(
  x: Float,
  y: Float,
  type: TileType,
  hp: Int,
  groundColor: Color,
  accentColor: Color,
  time: Float,
) {
  when (type) {
    TileType.SOLID_GROUND -> {
      drawRect(color = groundColor.copy(alpha = 0.85f), topLeft = Offset(x, y), size = Size(TILE_SIZE, TILE_SIZE))
      // Top cyber-brick rim
      drawRect(color = SpectrumWhite.copy(alpha = 0.35f), topLeft = Offset(x, y), size = Size(TILE_SIZE, 2f))
      drawRect(color = SpectrumVoid, topLeft = Offset(x + 2f, y + 4f), size = Size(12f, 10f))
      drawRect(color = groundColor.copy(alpha = 0.55f), topLeft = Offset(x + 4f, y + 6f), size = Size(8f, 6f))
    }

    TileType.STEEL_PLATFORM -> {
      drawRect(color = accentColor, topLeft = Offset(x, y), size = Size(TILE_SIZE, 5f))
      drawRect(color = SpectrumWhite.copy(alpha = 0.55f), topLeft = Offset(x, y), size = Size(TILE_SIZE, 1.5f))
      drawRect(color = SpectrumVoid, topLeft = Offset(x + 3f, y + 2f), size = Size(3f, 2f))
      drawRect(color = SpectrumVoid, topLeft = Offset(x + 10f, y + 2f), size = Size(3f, 2f))
    }

    TileType.CRATE -> {
      val c = if (hp <= 1) SpectrumOrange else SpectrumYellow
      drawRect(color = c, topLeft = Offset(x + 1f, y + 1f), size = Size(TILE_SIZE - 2f, TILE_SIZE - 2f))
      drawRect(color = SpectrumVoid, topLeft = Offset(x + 3f, y + 3f), size = Size(TILE_SIZE - 6f, TILE_SIZE - 6f))
      drawLine(
        color = c,
        start = Offset(x + 2f, y + 2f),
        end = Offset(x + TILE_SIZE - 2f, y + TILE_SIZE - 2f),
        strokeWidth = 1.6f,
      )
      drawLine(
        color = c,
        start = Offset(x + TILE_SIZE - 2f, y + 2f),
        end = Offset(x + 2f, y + TILE_SIZE - 2f),
        strokeWidth = 1.6f,
      )
    }

    TileType.BUNKER_WALL -> {
      val c = if (hp <= 3) SpectrumRed else SpectrumMagenta
      drawRect(color = c, topLeft = Offset(x, y), size = Size(TILE_SIZE, TILE_SIZE))
      drawRect(color = SpectrumVoid, topLeft = Offset(x + 2f, y + 2f), size = Size(5f, 5f))
      drawRect(color = SpectrumVoid, topLeft = Offset(x + 9f, y + 9f), size = Size(5f, 5f))
      drawRect(color = SpectrumWhite.copy(alpha = 0.5f), topLeft = Offset(x + 2f, y + 9f), size = Size(5f, 5f))
    }

    TileType.ACID -> {
      val wave = sin(time * 7f + x * 0.25f) * 1.5f
      drawRect(color = SpectrumGreen, topLeft = Offset(x, y + 4f + wave), size = Size(TILE_SIZE, TILE_SIZE - 4f))
      drawRect(color = SpectrumYellow, topLeft = Offset(x + 2f, y + 4f + wave), size = Size(6f, 2f))
    }

    TileType.SPIKES -> {
      drawRect(color = SpectrumRed, topLeft = Offset(x, y + 12f), size = Size(TILE_SIZE, 4f))
      for (i in 0 until 3) {
        val sx = x + 2f + i * 5f
        drawRect(color = SpectrumWhite, topLeft = Offset(sx, y + 5f), size = Size(2f, 7f))
        drawRect(color = SpectrumRed, topLeft = Offset(sx - 1f, y + 9f), size = Size(4f, 3f))
      }
    }
  }
}

private fun DrawScope.drawPodChamber(
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  primary: Color,
  secondary: Color,
  time: Float,
  isTeleporter: Boolean,
) {
  val pulse = 0.45f + 0.55f * sin(time * 7f)
  // Outer arch frame
  drawRect(color = primary, topLeft = Offset(x, y), size = Size(w, 4f))
  drawRect(color = primary, topLeft = Offset(x, y), size = Size(3f, h))
  drawRect(color = primary, topLeft = Offset(x + w - 3f, y), size = Size(3f, h))
  drawRect(color = secondary, topLeft = Offset(x, y + h - 4f), size = Size(w, 4f))

  // Inner shimmering energy core
  drawRect(
    color = secondary.copy(alpha = 0.28f * pulse),
    topLeft = Offset(x + 3f, y + 4f),
    size = Size(w - 6f, h - 8f),
  )
  // Vertical beam stripes
  for (i in 0 until 3) {
    val bx = x + 5f + i * ((w - 10f) / 2.2f)
    drawRect(
      color = (if (isTeleporter) SpectrumMagenta else SpectrumYellow).copy(alpha = 0.7f * pulse),
      topLeft = Offset(bx, y + 5f),
      size = Size(2f, h - 10f),
    )
  }
}
