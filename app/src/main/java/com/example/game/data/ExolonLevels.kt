package com.example.game.data

import com.example.game.model.DroneKind
import com.example.game.model.GRID_COLS
import com.example.game.model.HazardType
import com.example.game.model.LevelScreenData
import com.example.game.model.PickupItem
import com.example.game.model.PickupKind
import com.example.game.model.TILE_SIZE
import com.example.game.model.TileBlock
import com.example.game.model.TileType
import com.example.game.model.WorldObject
import com.example.game.model.ZoneTheme

object ExolonLevels {

  const val SCREENS_PER_ZONE = 6
  const val TOTAL_ZONES = 5

  val zoneThemes =
    listOf(
      ZoneTheme(1, "СЕКТОР 1: ПЕРИМЕТР", groundColorIndex = 1, accentColorIndex = 2, skyTintIndex = 0),
      ZoneTheme(2, "СЕКТОР 2: ЛАЗЕРНАЯ СЕТЬ", groundColorIndex = 4, accentColorIndex = 3, skyTintIndex = 1),
      ZoneTheme(3, "СЕКТОР 3: УЛЕЙ ЧУЖИХ", groundColorIndex = 2, accentColorIndex = 1, skyTintIndex = 2),
      ZoneTheme(4, "СЕКТОР 4: ЦИТАДЕЛЬ", groundColorIndex = 6, accentColorIndex = 5, skyTintIndex = 3),
      ZoneTheme(5, "СЕКТОР 5: ЯДРО РЕАКТОРА", groundColorIndex = 5, accentColorIndex = 3, skyTintIndex = 4),
    )

  fun getScreen(zoneIndex: Int, screenIndex: Int): LevelScreenData {
    val z = zoneIndex.coerceIn(0, TOTAL_ZONES - 1)
    val s = screenIndex.coerceIn(0, SCREENS_PER_ZONE - 1)
    return buildScreen(z, s)
  }

  private fun buildScreen(zone: Int, screen: Int): LevelScreenData {
    val tiles = mutableListOf<TileBlock>()
    val objects = mutableListOf<WorldObject>()
    val pickups = mutableListOf<PickupItem>()
    var nextId = zone * 1000 + screen * 100

    fun obj(
      type: HazardType,
      col: Int,
      row: Int,
      w: Float = 16f,
      h: Float = 16f,
      hp: Int = 3,
      gateTopRow: Int = 2,
    ) {
      objects +=
        WorldObject(
          id = nextId++,
          type = type,
          x = col * TILE_SIZE,
          y = row * TILE_SIZE + (16f - h),
          width = w,
          height = h,
          hp = hp,
          cooldown = 0.8f + (col % 3) * 0.45f,
          gateTopY = gateTopRow * TILE_SIZE,
        )
    }

    fun pick(kind: PickupKind, col: Int, row: Int) {
      pickups +=
        PickupItem(
          id = nextId++,
          kind = kind,
          x = col * TILE_SIZE + 2f,
          y = row * TILE_SIZE + 2f,
          bobPhase = col * 0.6f,
        )
    }

    fun floorWithGaps(gapCols: Set<Int> = emptySet(), hazardType: TileType = TileType.ACID) {
      for (c in 0 until GRID_COLS) {
        if (c in gapCols) {
          tiles += TileBlock(c, 9, hazardType)
        } else {
          tiles += TileBlock(c, 8, TileType.SOLID_GROUND)
          tiles += TileBlock(c, 9, TileType.SOLID_GROUND)
        }
      }
    }

    fun platform(startCol: Int, endCol: Int, row: Int) {
      for (c in startCol..endCol) {
        tiles += TileBlock(c, row, TileType.STEEL_PLATFORM)
      }
    }

    // Screen layouts tailored per (zone, screen) for escalating classic Exolon action
    val pattern = (zone * 6 + screen)
    when (screen) {
      0 -> {
        // Intro screen of each zone: warm-up mine, turret, ammo/exo pod
        floorWithGaps(
          gapCols = if (zone >= 2) setOf(7, 8) else emptySet(),
          hazardType = if (zone % 2 == 0) TileType.ACID else TileType.SPIKES,
        )
        platform(4, 7, 6)
        platform(10, 13, 5)
        tiles += TileBlock(9, 7, TileType.CRATE, hp = 2)
        if (zone == 0) {
          // Iconic first Exosuit Pod early in Sector 1 Screen 2 or bonus ammo here
          pick(PickupKind.CRYSTAL, 5, 5)
          obj(HazardType.MINE, col = 6, row = 7, w = 12f, h = 8f, hp = 1)
          obj(HazardType.TURRET_LEFT, col = 13, row = 7, w = 15f, h = 14f, hp = 2)
          pick(PickupKind.AMMO, 11, 4)
        } else {
          obj(HazardType.MINE, col = 5, row = 7, w = 12f, h = 8f, hp = 1)
          obj(HazardType.TURRET_LEFT, col = 12, row = 7, w = 15f, h = 14f, hp = 3)
          obj(HazardType.LASER_GATE, col = 14, row = 7, w = 12f, h = 14f, hp = 3, gateTopRow = 3)
          pick(PickupKind.AMMO, 5, 5)
          pick(PickupKind.GRENADE, 11, 4)
        }
      }

      1 -> {
        // Exosuit Pod + Laser Gate + Crates
        floorWithGaps(gapCols = setOf(5, 6), hazardType = TileType.SPIKES)
        platform(3, 7, 5)
        platform(10, 14, 6)
        obj(HazardType.EXO_POD, col = 2, row = 6, w = 22f, h = 32f, hp = 999)
        obj(HazardType.LASER_GATE, col = 9, row = 7, w = 12f, h = 14f, hp = 3, gateTopRow = 2)
        obj(HazardType.MINE, col = 12, row = 7, w = 12f, h = 8f, hp = 1)
        tiles += TileBlock(11, 5, TileType.CRATE, hp = 2)
        tiles += TileBlock(12, 5, TileType.CRATE, hp = 2)
        pick(PickupKind.CRYSTAL, 13, 5)
        if (zone >= 1) {
          obj(HazardType.TURRET_LEFT, col = 14, row = 5, w = 15f, h = 14f, hp = 2)
        }
      }

      2 -> {
        // Bunker Wall requiring Grenade (or heavy laser) + Rocket Silo
        floorWithGaps(gapCols = emptySet())
        platform(2, 5, 6)
        platform(9, 12, 5)
        pick(PickupKind.GRENADE, 3, 5)
        tiles += TileBlock(7, 7, TileType.BUNKER_WALL, hp = 8)
        tiles += TileBlock(7, 6, TileType.BUNKER_WALL, hp = 8)
        obj(HazardType.MINE, col = 5, row = 7, w = 12f, h = 8f, hp = 1)
        obj(HazardType.ROCKET_SILO, col = 11, row = 7, w = 16f, h = 16f, hp = 3)
        obj(HazardType.TURRET_LEFT, col = 14, row = 7, w = 15f, h = 14f, hp = 3)
        pick(PickupKind.AMMO, 10, 4)
      }

      3 -> {
        // Elevated Catwalk Gauntlet with Heavy Cannon
        floorWithGaps(gapCols = setOf(4, 5, 10, 11), hazardType = TileType.ACID)
        platform(3, 6, 6)
        platform(7, 12, 5)
        platform(13, 15, 6)
        obj(HazardType.MINE, col = 8, row = 7, w = 12f, h = 8f, hp = 1)
        obj(HazardType.HEAVY_CANNON, col = 13, row = 7, w = 18f, h = 16f, hp = 5)
        obj(HazardType.TURRET_LEFT, col = 11, row = 4, w = 15f, h = 14f, hp = 2)
        pick(PickupKind.CRYSTAL, 5, 5)
        pick(PickupKind.LIFE.takeIf { zone % 2 == 1 } ?: PickupKind.GRENADE, 9, 4)
      }

      4 -> {
        // Twin Laser Gates + Rocket Silos + Crates
        floorWithGaps(gapCols = setOf(6, 7), hazardType = TileType.SPIKES)
        platform(2, 5, 5)
        platform(9, 13, 6)
        tiles += TileBlock(4, 4, TileType.CRATE, hp = 2)
        obj(HazardType.LASER_GATE, col = 5, row = 7, w = 12f, h = 14f, hp = 3, gateTopRow = 2)
        obj(HazardType.ROCKET_SILO, col = 10, row = 7, w = 16f, h = 16f, hp = 3)
        obj(HazardType.HEAVY_CANNON, col = 14, row = 7, w = 18f, h = 16f, hp = 5)
        obj(HazardType.EXO_POD, col = 11, row = 4, w = 22f, h = 32f, hp = 999)
        pick(PickupKind.AMMO, 3, 4)
        pick(PickupKind.GRENADE, 12, 5)
      }

      else -> {
        // Screen 5 (Zone Finale): Defenses guarding the iconic Teleporter Chamber!
        floorWithGaps(gapCols = setOf(4, 5), hazardType = TileType.ACID)
        platform(3, 6, 6)
        platform(7, 10, 5)
        tiles += TileBlock(8, 7, TileType.BUNKER_WALL, hp = 8)
        tiles += TileBlock(8, 6, TileType.BUNKER_WALL, hp = 8)
        pick(PickupKind.GRENADE, 4, 5)
        pick(PickupKind.CRYSTAL, 9, 4)
        obj(HazardType.TURRET_LEFT, col = 10, row = 7, w = 15f, h = 14f, hp = 3)
        obj(HazardType.ROCKET_SILO, col = 12, row = 7, w = 16f, h = 16f, hp = 3)
        obj(HazardType.TELEPORTER, col = 14, row = 6, w = 24f, h = 32f, hp = 999)
      }
    }

    val droneKind =
      when ((pattern) % 3) {
        0 -> DroneKind.SINE_EYE
        1 -> DroneKind.SWOOP_HORNET
        else -> DroneKind.BOUNCE_ORB
      }

    return LevelScreenData(
      zoneIndex = zone,
      screenIndex = screen,
      tiles = tiles,
      objects = objects,
      pickups = pickups,
      spawnDrones = true,
      droneInterval = (2.7f - zone * 0.25f).coerceAtLeast(1.5f),
      maxDrones = (2 + zone / 2).coerceAtMost(4),
      preferredDroneKind = droneKind,
    )
  }
}
