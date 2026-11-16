package com.example.game.model

import androidx.compose.runtime.Immutable

const val GAME_WIDTH = 256f
const val GAME_HEIGHT = 160f
const val TILE_SIZE = 16f
const val GRID_COLS = 16
const val GRID_ROWS = 10

enum class ScreenPhase {
  TITLE,
  PLAYING,
  PAUSED,
  ZONE_CLEAR,
  GAME_OVER,
  VICTORY,
}

enum class TouchControlMode {
  ARCADE_PAD,
  SWIPE_TAP,
}

enum class TileType {
  SOLID_GROUND,
  STEEL_PLATFORM,
  CRATE,
  BUNKER_WALL,
  ACID,
  SPIKES,
}

enum class HazardType {
  MINE,
  TURRET_LEFT,
  HEAVY_CANNON,
  LASER_GATE,
  ROCKET_SILO,
  EXO_POD,
  TELEPORTER,
}

enum class DroneKind {
  SINE_EYE,
  SWOOP_HORNET,
  BOUNCE_ORB,
}

enum class PickupKind {
  AMMO,
  GRENADE,
  LIFE,
  CRYSTAL,
}

@Immutable
data class TileBlock(
  val col: Int,
  val row: Int,
  val type: TileType,
  val hp: Int = 2,
) {
  val x: Float get() = col * TILE_SIZE
  val y: Float get() = row * TILE_SIZE
}

@Immutable
data class WorldObject(
  val id: Int,
  val type: HazardType,
  val x: Float,
  val y: Float,
  val width: Float = 16f,
  val height: Float = 16f,
  val hp: Int = 3,
  val active: Boolean = true,
  val cooldown: Float = 1.2f,
  val gateTopY: Float = 0f,
)

@Immutable
data class DroneEnemy(
  val id: Int,
  val kind: DroneKind,
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val baseY: Float,
  val phase: Float = 0f,
  val hp: Int = 1,
)

@Immutable
data class Projectile(
  val id: Int,
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val isPlayer: Boolean,
  val isGrenade: Boolean = false,
  val isHoming: Boolean = false,
  val isTwin: Boolean = false,
  val life: Float = 3f,
)

@Immutable
data class PickupItem(
  val id: Int,
  val kind: PickupKind,
  val x: Float,
  val y: Float,
  val bobPhase: Float = 0f,
)

@Immutable
data class Particle(
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val colorIndex: Int,
  val life: Float,
  val maxLife: Float,
  val size: Float = 2.5f,
)

@Immutable
data class PlayerState(
  val x: Float = 18f,
  val y: Float = 112f,
  val vx: Float = 0f,
  val vy: Float = 0f,
  val facingRight: Boolean = true,
  val onGround: Boolean = true,
  val isDucking: Boolean = false,
  val isExoSuit: Boolean = false,
  val exoShieldHits: Int = 0,
  val animTimer: Float = 0f,
  val fireCooldown: Float = 0f,
  val grenadeCooldown: Float = 0f,
  val invulnerableTimer: Float = 0f,
  val teleportAnimTimer: Float = 0f,
) {
  val width: Float get() = if (isExoSuit) 17f else 13f
  val height: Float
    get() =
      when {
        isDucking -> 11f
        isExoSuit -> 21f
        else -> 19f
      }
}

@Immutable
data class ZoneTheme(
  val zoneNumber: Int,
  val name: String,
  val groundColorIndex: Int,
  val accentColorIndex: Int,
  val skyTintIndex: Int,
)

@Immutable
data class LevelScreenData(
  val zoneIndex: Int,
  val screenIndex: Int,
  val tiles: List<TileBlock>,
  val objects: List<WorldObject>,
  val pickups: List<PickupItem>,
  val spawnDrones: Boolean = true,
  val droneInterval: Float = 2.8f,
  val maxDrones: Int = 3,
  val preferredDroneKind: DroneKind = DroneKind.SINE_EYE,
)

@Immutable
data class HighScoreEntry(
  val name: String,
  val score: Int,
  val isPlayer: Boolean = false,
)

@Immutable
data class GameUiState(
  val phase: ScreenPhase = ScreenPhase.TITLE,
  val zoneIndex: Int = 0,
  val screenIndex: Int = 0,
  val zoneName: String = "СЕКТОР 1: ПЕРИМЕТР",
  val score: Int = 0,
  val highScore: Int = 1000,
  val lives: Int = 5,
  val ammo: Int = 99,
  val grenades: Int = 10,
  val player: PlayerState = PlayerState(),
  val tiles: List<TileBlock> = emptyList(),
  val objects: List<WorldObject> = emptyList(),
  val drones: List<DroneEnemy> = emptyList(),
  val projectiles: List<Projectile> = emptyList(),
  val pickups: List<PickupItem> = emptyList(),
  val particles: List<Particle> = emptyList(),
  val screenShake: Float = 0f,
  val bannerText: String? = null,
  val bannerTimer: Float = 0f,
  val soundEnabled: Boolean = true,
  val crtScanlines: Boolean = true,
  val controlMode: TouchControlMode = TouchControlMode.ARCADE_PAD,
  val highScores: List<HighScoreEntry> = emptyList(),
  val totalScreensCleared: Int = 0,
  val elapsedSeconds: Float = 0f,
  val hasActiveSession: Boolean = false,
)

data class TouchInputState(
  var left: Boolean = false,
  var right: Boolean = false,
  var duck: Boolean = false,
  var jump: Boolean = false,
  var fire: Boolean = false,
  var grenade: Boolean = false,
) {
  fun clear() {
    left = false
    right = false
    duck = false
    jump = false
    fire = false
    grenade = false
  }
}
