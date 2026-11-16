package com.example.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.RetroAudioEngine
import com.example.game.data.ExolonLevels
import com.example.game.data.ExolonStorage
import com.example.game.model.DroneEnemy
import com.example.game.model.DroneKind
import com.example.game.model.GAME_HEIGHT
import com.example.game.model.GAME_WIDTH
import com.example.game.model.GameUiState
import com.example.game.model.HazardType
import com.example.game.model.LevelScreenData
import com.example.game.model.Particle
import com.example.game.model.PickupItem
import com.example.game.model.PickupKind
import com.example.game.model.PlayerState
import com.example.game.model.Projectile
import com.example.game.model.ScreenPhase
import com.example.game.model.TILE_SIZE
import com.example.game.model.TileBlock
import com.example.game.model.TileType
import com.example.game.model.TouchControlMode
import com.example.game.model.TouchInputState
import com.example.game.model.WorldObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExolonViewModel(application: Application) : AndroidViewModel(application) {

  private val storage = ExolonStorage(application.applicationContext)
  private val audio = RetroAudioEngine()

  val touchInput = TouchInputState()

  private val _uiState = MutableStateFlow(GameUiState())
  val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

  private var loopJob: Job? = null
  private var nextEntityId = 10_000
  private var droneSpawnTimer = 1.6f
  private var fireHoldTimer = 0f
  private var fireHoldGrenadeSent = false
  private var currentScreenData: LevelScreenData = ExolonLevels.getScreen(0, 0)

  // Cache modified screen states within current run so destroyed crates/turrets stay cleared on backtrack
  private val screenStateCache = mutableMapOf<Pair<Int, Int>, Triple<List<TileBlock>, List<WorldObject>, List<PickupItem>>>()

  init {
    viewModelScope.launch {
      storage.prefsFlow.collect { prefs ->
        audio.enabled = prefs.soundEnabled
        val topScore = prefs.highScores.maxOfOrNull { it.score } ?: 1000
        _uiState.update { state ->
          state.copy(
            soundEnabled = prefs.soundEnabled,
            crtScanlines = prefs.crtScanlines,
            controlMode = prefs.controlMode,
            highScores = prefs.highScores,
            highScore = maxOf(state.score, topScore),
          )
        }
      }
    }
    loadScreenIntoState(0, 0, resetPlayerPos = true)
  }

  fun startNewGame() {
    touchInput.clear()
    screenStateCache.clear()
    fireHoldTimer = 0f
    fireHoldGrenadeSent = false
    val topHi = _uiState.value.highScores.maxOfOrNull { it.score } ?: 1000
    _uiState.update {
      it.copy(
        phase = ScreenPhase.PLAYING,
        score = 0,
        highScore = topHi,
        lives = 5,
        ammo = 99,
        grenades = 10,
        zoneIndex = 0,
        screenIndex = 0,
        totalScreensCleared = 0,
        player = PlayerState(x = 18f, y = 100f, invulnerableTimer = 1.5f),
        hasActiveSession = true,
        bannerText = "СЕКТОР 1: ВПЕРЁД!",
        bannerTimer = 2.0f,
      )
    }
    loadScreenIntoState(0, 0, resetPlayerPos = true)
    startLoop()
  }

  fun resumeGame() {
    if (_uiState.value.hasActiveSession) {
      touchInput.clear()
      _uiState.update { it.copy(phase = ScreenPhase.PLAYING) }
      startLoop()
    }
  }

  fun pauseGame() {
    if (_uiState.value.phase == ScreenPhase.PLAYING) {
      touchInput.clear()
      loopJob?.cancel()
      _uiState.update { it.copy(phase = ScreenPhase.PAUSED) }
    }
  }

  fun returnToTitle() {
    touchInput.clear()
    loopJob?.cancel()
    _uiState.update { it.copy(phase = ScreenPhase.TITLE) }
  }

  fun toggleSound() {
    val next = !_uiState.value.soundEnabled
    audio.enabled = next
    _uiState.update { it.copy(soundEnabled = next) }
    viewModelScope.launch { storage.setSoundEnabled(next) }
  }

  fun toggleCrt() {
    val next = !_uiState.value.crtScanlines
    _uiState.update { it.copy(crtScanlines = next) }
    viewModelScope.launch { storage.setCrtScanlines(next) }
  }

  fun toggleControlMode() {
    val next =
      if (_uiState.value.controlMode == TouchControlMode.ARCADE_PAD) {
        TouchControlMode.SWIPE_TAP
      } else {
        TouchControlMode.ARCADE_PAD
      }
    touchInput.clear()
    _uiState.update { it.copy(controlMode = next) }
    viewModelScope.launch { storage.setControlMode(next) }
  }

  fun submitScoreName(name: String) {
    val finalScore = _uiState.value.score
    if (finalScore <= 0) return
    viewModelScope.launch {
      val updated = storage.recordScore(name, finalScore)
      _uiState.update {
        it.copy(
          highScores = updated,
          highScore = maxOf(it.highScore, updated.maxOfOrNull { e -> e.score } ?: 1000),
        )
      }
    }
  }

  private fun startLoop() {
    loopJob?.cancel()
    loopJob =
      viewModelScope.launch {
        var lastNs = System.nanoTime()
        while (isActive) {
          val nowNs = System.nanoTime()
          val rawDt = (nowNs - lastNs) / 1_000_000_000f
          lastNs = nowNs
          val dt = rawDt.coerceIn(0.001f, 0.035f)
          if (_uiState.value.phase == ScreenPhase.PLAYING) {
            stepSimulation(dt)
          }
          delay(16L)
        }
      }
  }

  private fun loadScreenIntoState(zone: Int, screen: Int, resetPlayerPos: Boolean, entryX: Float = 16f) {
    currentScreenData = ExolonLevels.getScreen(zone, screen)
    val cached = screenStateCache[zone to screen]
    val tiles = cached?.first ?: currentScreenData.tiles
    val objects = cached?.second ?: currentScreenData.objects
    val pickups = cached?.third ?: currentScreenData.pickups
    val theme = ExolonLevels.zoneThemes[zone.coerceIn(0, ExolonLevels.zoneThemes.lastIndex)]
    droneSpawnTimer = 1.1f

    _uiState.update { s ->
      val nextPlayer =
        if (resetPlayerPos) {
          s.player.copy(x = entryX, y = 104f, vx = 0f, vy = 0f, onGround = false)
        } else {
          s.player.copy(x = entryX)
        }
      s.copy(
        zoneIndex = zone,
        screenIndex = screen,
        zoneName = theme.name,
        tiles = tiles,
        objects = objects,
        pickups = pickups,
        drones = emptyList(),
        projectiles = emptyList(),
        particles = emptyList(),
        player = nextPlayer,
      )
    }
  }

  private fun saveCurrentScreenToCache(state: GameUiState) {
    screenStateCache[state.zoneIndex to state.screenIndex] =
      Triple(state.tiles, state.objects, state.pickups)
  }

  private fun stepSimulation(dt: Float) {
    var state = _uiState.value
    var player = state.player
    var tiles = state.tiles.toMutableList()
    var objects = state.objects.toMutableList()
    var drones = state.drones.toMutableList()
    var projectiles = state.projectiles.toMutableList()
    var pickups = state.pickups.toMutableList()
    val particles = state.particles.toMutableList()
    var score = state.score
    var lives = state.lives
    var ammo = state.ammo
    var grenades = state.grenades
    var shake = (state.screenShake - dt * 2.5f).coerceAtLeast(0f)
    var bannerText = state.bannerText
    var bannerTimer = (state.bannerTimer - dt).coerceAtLeast(0f)
    if (bannerTimer <= 0f) bannerText = null

    val elapsed = state.elapsedSeconds + dt

    // Handle active teleport beam-out sequence
    if (player.teleportAnimTimer > 0f) {
      val nextTp = player.teleportAnimTimer - dt
      if (nextTp <= 0f) {
        advanceToNextZone(state)
        return
      }
      _uiState.update {
        it.copy(
          player = player.copy(teleportAnimTimer = nextTp),
          elapsedSeconds = elapsed,
          particles = particles.mapNotNull { pt -> updateParticle(pt, dt) },
        )
      }
      return
    }

    // 1. Player Input & Horizontal Physics
    val prevHeight = player.height
    val wantsDuck = touchInput.duck && player.onGround
    val moveDir =
      when {
        touchInput.left && !touchInput.right -> -1
        touchInput.right && !touchInput.left -> 1
        else -> 0
      }

    val moveSpeed = if (wantsDuck) 34f else 88f
    var vx = moveDir * moveSpeed
    var facingRight =
      when {
        moveDir > 0 -> true
        moveDir < 0 -> false
        else -> player.facingRight
      }

    // Adjust Y cleanly when toggling ducking so feet stay planted
    var py = player.y
    val newTempHeight =
      when {
        wantsDuck -> 11f
        player.isExoSuit -> 21f
        else -> 19f
      }
    if (newTempHeight != prevHeight) {
      py += (prevHeight - newTempHeight)
    }

    var vy = player.vy
    var onGround = player.onGround

    if (touchInput.jump && onGround && !wantsDuck) {
      vy = -218f
      onGround = false
      audio.playJump()
    }

    // Gravity
    vy = (vy + 560f * dt).coerceAtMost(300f)

    // Move X & resolve solid wall collisions
    var px = player.x + vx * dt
    val pw = player.width
    val ph = newTempHeight

    for (tile in tiles) {
      if (tile.type == TileType.SOLID_GROUND || tile.type == TileType.CRATE || tile.type == TileType.BUNKER_WALL) {
        if (rectsOverlap(px, py, pw, ph, tile.x, tile.y, TILE_SIZE, TILE_SIZE)) {
          if (vx > 0f) {
            px = tile.x - pw
          } else if (vx < 0f) {
            px = tile.x + TILE_SIZE
          }
          vx = 0f
        }
      }
    }

    // Move Y & resolve ground / platform collisions
    val prevBottom = py + ph
    py += vy * dt
    onGround = false

    for (tile in tiles) {
      val isSolid =
        tile.type == TileType.SOLID_GROUND || tile.type == TileType.CRATE || tile.type == TileType.BUNKER_WALL
      val isPlatform = tile.type == TileType.STEEL_PLATFORM

      if (isSolid) {
        if (rectsOverlap(px, py, pw, ph, tile.x, tile.y, TILE_SIZE, TILE_SIZE)) {
          if (vy >= 0f) {
            py = tile.y - ph
            vy = 0f
            onGround = true
          } else {
            py = tile.y + TILE_SIZE
            vy = 0f
          }
        }
      } else if (isPlatform && vy >= 0f && !touchInput.duck) {
        if (
          px + pw > tile.x + 1f &&
            px < tile.x + TILE_SIZE - 1f &&
            prevBottom <= tile.y + 4f &&
            py + ph >= tile.y
        ) {
          py = tile.y - ph
          vy = 0f
          onGround = true
        }
      }
    }

    // Check screen edge transitions!
    if (px + pw >= GAME_WIDTH - 1f) {
      if (state.screenIndex < ExolonLevels.SCREENS_PER_ZONE - 1) {
        saveCurrentScreenToCache(state.copy(tiles = tiles, objects = objects, pickups = pickups))
        val nextScreen = state.screenIndex + 1
        _uiState.update {
          it.copy(
            score = score + 50,
            highScore = maxOf(it.highScore, score + 50),
            ammo = ammo,
            grenades = grenades,
            lives = lives,
            player = player.copy(facingRight = facingRight, isDucking = wantsDuck),
            totalScreensCleared = maxOf(it.totalScreensCleared, state.zoneIndex * ExolonLevels.SCREENS_PER_ZONE + nextScreen),
          )
        }
        loadScreenIntoState(state.zoneIndex, nextScreen, resetPlayerPos = false, entryX = 6f)
        return
      } else {
        px = GAME_WIDTH - pw - 1f
      }
    } else if (px <= 1f) {
      if (state.screenIndex > 0) {
        saveCurrentScreenToCache(state.copy(tiles = tiles, objects = objects, pickups = pickups))
        val prevScreen = state.screenIndex - 1
        _uiState.update {
          it.copy(
            ammo = ammo,
            grenades = grenades,
            lives = lives,
            player = player.copy(facingRight = facingRight, isDucking = wantsDuck),
          )
        }
        loadScreenIntoState(
          state.zoneIndex,
          prevScreen,
          resetPlayerPos = false,
          entryX = GAME_WIDTH - pw - 8f,
        )
        return
      } else {
        px = 1f
      }
    }

    val nextAnimTimer = if (abs(vx) > 5f && onGround) player.animTimer + dt else 0f
    var fireCooldown = (player.fireCooldown - dt).coerceAtLeast(0f)
    var grenadeCooldown = (player.grenadeCooldown - dt).coerceAtLeast(0f)
    var invuln = (player.invulnerableTimer - dt).coerceAtLeast(0f)

    // 2. Player Weapons (Laser + Hold-to-Grenade + Dedicated Grenade Button)
    if (touchInput.fire) {
      fireHoldTimer += dt
      if (fireHoldTimer >= 0.45f && !fireHoldGrenadeSent && grenades > 0 && grenadeCooldown <= 0f) {
        fireHoldGrenadeSent = true
        grenades -= 1
        grenadeCooldown = 0.40f
        projectiles +=
          Projectile(
            id = nextEntityId++,
            x = px + (if (facingRight) pw else 0f),
            y = py + 4f,
            vx = if (facingRight) 115f else -115f,
            vy = -138f,
            isPlayer = true,
            isGrenade = true,
            life = 2.4f,
          )
        audio.playGrenadeThrow()
      }
    } else {
      fireHoldTimer = 0f
      fireHoldGrenadeSent = false
    }

    if (touchInput.grenade && grenades > 0 && grenadeCooldown <= 0f) {
      grenades -= 1
      grenadeCooldown = 0.40f
      projectiles +=
        Projectile(
          id = nextEntityId++,
          x = px + (if (facingRight) pw else 0f),
          y = py + 4f,
          vx = if (facingRight) 118f else -118f,
          vy = -142f,
          isPlayer = true,
          isGrenade = true,
          life = 2.4f,
        )
      audio.playGrenadeThrow()
    }

    if (touchInput.fire && fireCooldown <= 0f) {
      val dir = if (facingRight) 1f else -1f
      val hasAmmo = ammo > 0
      if (hasAmmo) ammo -= 1
      fireCooldown =
        when {
          !hasAmmo -> 0.38f
          player.isExoSuit -> 0.13f
          else -> 0.17f
        }
      val muzzleX = if (facingRight) px + pw + 2f else px - 4f
      if (player.isExoSuit && hasAmmo) {
        projectiles +=
          Projectile(
            id = nextEntityId++,
            x = muzzleX,
            y = py + 5f,
            vx = dir * 240f,
            vy = 0f,
            isPlayer = true,
            isTwin = true,
            life = 1.2f,
          )
        projectiles +=
          Projectile(
            id = nextEntityId++,
            x = muzzleX,
            y = py + 10f,
            vx = dir * 240f,
            vy = 0f,
            isPlayer = true,
            isTwin = true,
            life = 1.2f,
          )
        audio.playLaser(isTwin = true)
      } else {
        projectiles +=
          Projectile(
            id = nextEntityId++,
            x = muzzleX,
            y = py + (if (wantsDuck) 4f else 7f),
            vx = dir * 225f,
            vy = 0f,
            isPlayer = true,
            life = 1.2f,
          )
        audio.playLaser(isTwin = false)
      }
    }

    // 3. World Objects (Turrets, Silos, Exo Pod, Teleporter)
    for (idx in objects.indices) {
      val obj = objects[idx]
      if (!obj.active) continue
      when (obj.type) {
        HazardType.TURRET_LEFT, HazardType.HEAVY_CANNON -> {
          val nextCd = obj.cooldown - dt
          if (nextCd <= 0f) {
            val fireRate = (1.85f - state.zoneIndex * 0.15f).coerceAtLeast(1.05f)
            objects[idx] = obj.copy(cooldown = fireRate)
            val muzzleY =
              if (obj.type == HazardType.HEAVY_CANNON) obj.y + 4f else obj.y + 4f
            projectiles +=
              Projectile(
                id = nextEntityId++,
                x = obj.x - 3f,
                y = muzzleY,
                vx = -135f - state.zoneIndex * 10f,
                vy = 0f,
                isPlayer = false,
                life = 2.5f,
              )
          } else {
            objects[idx] = obj.copy(cooldown = nextCd)
          }
        }

        HazardType.ROCKET_SILO -> {
          val nextCd = obj.cooldown - dt
          if (nextCd <= 0f) {
            objects[idx] = obj.copy(cooldown = 2.6f)
            projectiles +=
              Projectile(
                id = nextEntityId++,
                x = obj.x + obj.width * 0.5f,
                y = obj.y - 4f,
                vx = -40f,
                vy = -85f,
                isPlayer = false,
                isHoming = true,
                life = 3.8f,
              )
          } else {
            objects[idx] = obj.copy(cooldown = nextCd)
          }
        }

        HazardType.EXO_POD -> {
          if (rectsOverlap(px, py, pw, ph, obj.x + 2f, obj.y + 4f, obj.width - 4f, obj.height - 4f)) {
            objects[idx] = obj.copy(active = false)
            player = player.copy(isExoSuit = true, exoShieldHits = 1)
            ammo = (ammo + 25).coerceAtMost(99)
            grenades = (grenades + 3).coerceAtMost(20)
            score += 250
            bannerText = "ЭКЗО-КОСТЮМ АКТИВИРОВАН!"
            bannerTimer = 2.2f
            audio.playExoPowerUp()
            spawnBurst(particles, obj.x + obj.width * 0.5f, obj.y + obj.height * 0.5f, 3, 18)
          }
        }

        HazardType.TELEPORTER -> {
          if (rectsOverlap(px, py, pw, ph, obj.x + 3f, obj.y + 4f, obj.width - 6f, obj.height - 4f)) {
            player = player.copy(x = px, y = py, teleportAnimTimer = 0.85f)
            audio.playTeleport()
            spawnBurst(particles, obj.x + obj.width * 0.5f, obj.y + obj.height * 0.5f, 1, 24)
            _uiState.update {
              it.copy(
                player = player,
                score = score,
                ammo = ammo,
                grenades = grenades,
                particles = particles,
                bannerText = "ТЕЛЕПОРТАЦИЯ...",
                bannerTimer = 1.2f,
              )
            }
            return
          }
        }

        else -> Unit
      }
    }

    // 4. Spawn & Update Flying Alien Drones
    droneSpawnTimer -= dt
    if (currentScreenData.spawnDrones && droneSpawnTimer <= 0f && drones.size < currentScreenData.maxDrones) {
      droneSpawnTimer = currentScreenData.droneInterval
      val spawnY = 24f + Random.nextFloat() * 76f
      val speed = -(48f + state.zoneIndex * 7f)
      drones +=
        DroneEnemy(
          id = nextEntityId++,
          kind = currentScreenData.preferredDroneKind,
          x = GAME_WIDTH + 6f,
          y = spawnY,
          vx = speed,
          vy = if (currentScreenData.preferredDroneKind == DroneKind.BOUNCE_ORB) 55f else 0f,
          baseY = spawnY,
          phase = Random.nextFloat() * 6.28f,
          hp = if (state.zoneIndex >= 3) 2 else 1,
        )
    }

    val updatedDrones = mutableListOf<DroneEnemy>()
    for (d in drones) {
      val nextPhase = d.phase + dt * 4.5f
      var nx = d.x + d.vx * dt
      var ny = d.y
      var nvy = d.vy
      when (d.kind) {
        DroneKind.SINE_EYE -> ny = (d.baseY + sin(nextPhase) * 16f).coerceIn(12f, 114f)
        DroneKind.SWOOP_HORNET -> {
          val targetY = py + ph * 0.4f
          ny += (targetY - ny).coerceIn(-38f, 38f) * dt * 1.4f
        }
        DroneKind.BOUNCE_ORB -> {
          ny += nvy * dt
          if (ny < 14f) {
            ny = 14f
            nvy = abs(nvy)
          } else if (ny > 114f) {
            ny = 114f
            nvy = -abs(nvy)
          }
        }
      }
      if (nx > -18f) {
        updatedDrones += d.copy(x = nx, y = ny, vy = nvy, phase = nextPhase)
      }
    }
    drones = updatedDrones

    // 5. Update Projectiles & Check Projectile Collisions
    val survivingProjectiles = mutableListOf<Projectile>()
    for (proj in projectiles) {
      var pnx = proj.x + proj.vx * dt
      var pny = proj.y + proj.vy * dt
      var pvx = proj.vx
      var pvy = proj.vy
      val nextLife = proj.life - dt

      if (proj.isGrenade) {
        pvy += 360f * dt
      } else if (proj.isHoming) {
        val angle = atan2((py + ph * 0.5f) - pny, (px + pw * 0.5f) - pnx)
        val speed = 82f + state.zoneIndex * 6f
        pvx += (cos(angle) * speed - pvx) * dt * 2.4f
        pvy += (sin(angle) * speed - pvy) * dt * 2.4f
      }

      if (nextLife <= 0f || pnx < -12f || pnx > GAME_WIDTH + 12f || pny > GAME_HEIGHT + 8f || pny < -20f) {
        continue
      }

      var consumed = false

      if (proj.isPlayer) {
        // Check hit on tiles (crates, bunker walls, solid ground for grenades)
        val tileIter = tiles.listIterator()
        while (tileIter.hasNext() && !consumed) {
          val t = tileIter.next()
          if (rectsOverlap(pnx - 2f, pny - 2f, 4f, 4f, t.x, t.y, TILE_SIZE, TILE_SIZE)) {
            when (t.type) {
              TileType.CRATE -> {
                consumed = true
                if (proj.isGrenade) {
                  tileIter.remove()
                  score += 50
                  pickups += PickupItem(nextEntityId++, PickupKind.CRYSTAL, t.x + 2f, t.y + 2f)
                } else {
                  val rem = t.hp - 1
                  if (rem <= 0) {
                    tileIter.remove()
                    score += 50
                    pickups += PickupItem(nextEntityId++, PickupKind.AMMO, t.x + 2f, t.y + 2f)
                  } else {
                    tileIter.set(t.copy(hp = rem))
                  }
                }
              }
              TileType.BUNKER_WALL -> {
                consumed = true
                val dmg = if (proj.isGrenade) 99 else 1
                val rem = t.hp - dmg
                if (rem <= 0) {
                  tileIter.remove()
                  score += 100
                } else {
                  tileIter.set(t.copy(hp = rem))
                }
              }
              TileType.SOLID_GROUND -> {
                consumed = true
              }
              else -> Unit
            }
          }
        }

        // Check hit on hazards / turrets / silos / mines / laser gates
        if (!consumed) {
          for (i in objects.indices) {
            val obj = objects[i]
            if (!obj.active || obj.type == HazardType.EXO_POD || obj.type == HazardType.TELEPORTER) continue
            if (obj.type == HazardType.MINE && !proj.isGrenade) continue
            if (rectsOverlap(pnx - 3f, pny - 3f, 6f, 6f, obj.x, obj.y, obj.width, obj.height)) {
              consumed = true
              val dmg = if (proj.isGrenade) 99 else 1
              val rem = obj.hp - dmg
              if (rem <= 0) {
                objects[i] = obj.copy(active = false)
                score +=
                  when (obj.type) {
                    HazardType.HEAVY_CANNON -> 300
                    HazardType.ROCKET_SILO -> 250
                    HazardType.LASER_GATE -> 200
                    HazardType.TURRET_LEFT -> 150
                    else -> 100
                  }
                spawnBurst(particles, obj.x + obj.width * 0.5f, obj.y + obj.height * 0.5f, 3, 14)
                audio.playExplosion(heavy = proj.isGrenade || obj.type == HazardType.HEAVY_CANNON)
                if (proj.isGrenade) shake = 0.6f
              } else {
                objects[i] = obj.copy(hp = rem)
                spawnBurst(particles, pnx, pny, 1, 5)
              }
              break
            }
          }
        }

        // Check hit on drones
        if (!consumed) {
          val dIter = drones.listIterator()
          while (dIter.hasNext() && !consumed) {
            val d = dIter.next()
            if (rectsOverlap(pnx - 3f, pny - 3f, 6f, 6f, d.x, d.y, 14f, 11f)) {
              consumed = true
              val rem = d.hp - (if (proj.isGrenade) 99 else 1)
              if (rem <= 0) {
                dIter.remove()
                score += 100
                spawnBurst(particles, d.x + 7f, d.y + 5f, 4, 12)
                audio.playExplosion(heavy = false)
              } else {
                dIter.set(d.copy(hp = rem))
              }
            }
          }
        }

        // Player laser can also shoot down homing missiles!
        if (!consumed && !proj.isGrenade) {
          for (j in projectiles.indices) {
            val other = projectiles[j]
            if (!other.isPlayer && other.isHoming && rectsOverlap(pnx - 4f, pny - 4f, 8f, 8f, other.x - 3f, other.y - 3f, 6f, 6f)) {
              consumed = true
              projectiles[j] = other.copy(life = 0f)
              score += 100
              spawnBurst(particles, other.x, other.y, 8, 10)
              audio.playExplosion(heavy = false)
              break
            }
          }
        }

        // Grenade AoE blast on impact!
        if (consumed && proj.isGrenade) {
          shake = maxOf(shake, 0.7f)
          audio.playExplosion(heavy = true)
          spawnBurst(particles, pnx, pny, 5, 18)
          // AoE destroys nearby mines, walls, crates, turrets, drones within 26px
          val aoeTiles = tiles.listIterator()
          while (aoeTiles.hasNext()) {
            val t = aoeTiles.next()
            if (
              (t.type == TileType.BUNKER_WALL || t.type == TileType.CRATE) &&
                dist(pnx, pny, t.x + 8f, t.y + 8f) < 28f
            ) {
              aoeTiles.remove()
              score += 75
            }
          }
          for (i in objects.indices) {
            val obj = objects[i]
            if (
              obj.active &&
                obj.type != HazardType.EXO_POD &&
                obj.type != HazardType.TELEPORTER &&
                dist(pnx, pny, obj.x + obj.width * 0.5f, obj.y + obj.height * 0.5f) < 28f
            ) {
              objects[i] = obj.copy(active = false)
              score += 150
            }
          }
          drones.removeAll { d ->
            val hit = dist(pnx, pny, d.x + 7f, d.y + 5f) < 30f
            if (hit) score += 100
            hit
          }
        }
      }

      if (!consumed) {
        survivingProjectiles +=
          proj.copy(x = pnx, y = pny, vx = pvx, vy = pvy, life = nextLife)
      }
    }
    projectiles = survivingProjectiles

    // 6. Pickups Collection
    val pIter = pickups.listIterator()
    while (pIter.hasNext()) {
      val item = pIter.next()
      if (rectsOverlap(px, py, pw, ph, item.x, item.y, 11f, 11f)) {
        pIter.remove()
        audio.playPickup()
        when (item.kind) {
          PickupKind.AMMO -> {
            ammo = (ammo + 20).coerceAtMost(99)
            score += 50
          }
          PickupKind.GRENADE -> {
            grenades = (grenades + 5).coerceAtMost(20)
            score += 75
          }
          PickupKind.LIFE -> {
            lives = (lives + 1).coerceAtMost(9)
            score += 200
            bannerText = "+1 ЖИЗНЬ!"
            bannerTimer = 1.6f
          }
          PickupKind.CRYSTAL -> {
            score += 300
          }
        }
        spawnBurst(particles, item.x + 5f, item.y + 5f, 1, 8)
      }
    }

    // 7. Player Hazard & Enemy Collision Check
    var playerHit = false
    if (invuln <= 0f) {
      // Acid & Spikes
      for (t in tiles) {
        if (t.type == TileType.ACID || t.type == TileType.SPIKES) {
          if (rectsOverlap(px + 2f, py + 2f, pw - 4f, ph - 2f, t.x + 2f, t.y + 4f, TILE_SIZE - 4f, TILE_SIZE - 4f)) {
            playerHit = true
            break
          }
        }
      }
      // Mines & Laser Gate Beams
      if (!playerHit) {
        for (obj in objects) {
          if (!obj.active) continue
          when (obj.type) {
            HazardType.MINE -> {
              if (rectsOverlap(px + 2f, py + 2f, pw - 4f, ph - 2f, obj.x + 2f, obj.y + 1f, obj.width - 4f, obj.height - 1f)) {
                playerHit = true
              }
            }
            HazardType.LASER_GATE -> {
              if (rectsOverlap(px + 2f, py, pw - 4f, ph, obj.x + 3f, obj.gateTopY, 6f, obj.y - obj.gateTopY)) {
                playerHit = true
              }
            }
            else -> Unit
          }
          if (playerHit) break
        }
      }
      // Flying Drones
      if (!playerHit) {
        for (d in drones) {
          if (rectsOverlap(px + 2f, py + 2f, pw - 4f, ph - 4f, d.x + 2f, d.y + 2f, 10f, 7f)) {
            playerHit = true
            break
          }
        }
      }
      // Enemy Bolts / Homing Missiles
      if (!playerHit) {
        val eProjIter = projectiles.listIterator()
        while (eProjIter.hasNext()) {
          val ep = eProjIter.next()
          if (!ep.isPlayer && rectsOverlap(px + 2f, py + 2f, pw - 4f, ph - 3f, ep.x - 2f, ep.y - 2f, 4f, 4f)) {
            eProjIter.remove()
            playerHit = true
            break
          }
        }
      }
    }

    if (playerHit) {
      audio.playPlayerHit()
      shake = 0.85f
      spawnBurst(particles, px + pw * 0.5f, py + ph * 0.5f, 5, 20)
      if (player.isExoSuit && player.exoShieldHits > 0) {
        player = player.copy(isExoSuit = false, exoShieldHits = 0, invulnerableTimer = 1.8f)
        bannerText = "ЭКЗО-ЩИТ ПРОБИТ!"
        bannerTimer = 1.6f
      } else {
        lives -= 1
        if (lives <= 0) {
          loopJob?.cancel()
          _uiState.update {
            it.copy(
              phase = ScreenPhase.GAME_OVER,
              lives = 0,
              score = score,
              highScore = maxOf(it.highScore, score),
              ammo = ammo,
              grenades = grenades,
              particles = particles,
              hasActiveSession = false,
            )
          }
          return
        } else {
          // Respawn safely on current screen
          px = 16f
          py = 96f
          vx = 0f
          vy = 0f
          invuln = 2.0f
          bannerText = "ОСТАЛОСЬ ЖИЗНЕЙ: $lives"
          bannerTimer = 1.5f
        }
      }
    }

    val updatedParticles = particles.mapNotNull { updateParticle(it, dt) }

    _uiState.update {
      it.copy(
        score = score,
        highScore = maxOf(it.highScore, score),
        lives = lives,
        ammo = ammo,
        grenades = grenades,
        tiles = tiles,
        objects = objects,
        drones = drones,
        projectiles = projectiles,
        pickups = pickups,
        particles = updatedParticles,
        screenShake = shake,
        bannerText = bannerText,
        bannerTimer = bannerTimer,
        elapsedSeconds = elapsed,
        player =
          player.copy(
            x = px,
            y = py,
            vx = vx,
            vy = vy,
            facingRight = facingRight,
            onGround = onGround,
            isDucking = wantsDuck,
            animTimer = nextAnimTimer,
            fireCooldown = fireCooldown,
            grenadeCooldown = grenadeCooldown,
            invulnerableTimer = invuln,
          ),
      )
    }
  }

  private fun advanceToNextZone(state: GameUiState) {
    val bonusScore = state.score + 1000
    val nextZone = state.zoneIndex + 1
    if (nextZone >= ExolonLevels.TOTAL_ZONES) {
      loopJob?.cancel()
      _uiState.update {
        it.copy(
          phase = ScreenPhase.VICTORY,
          score = bonusScore,
          highScore = maxOf(it.highScore, bonusScore),
          hasActiveSession = false,
          player = it.player.copy(teleportAnimTimer = 0f),
        )
      }
    } else {
      _uiState.update {
        it.copy(
          score = bonusScore,
          highScore = maxOf(it.highScore, bonusScore),
          ammo = (it.ammo + 30).coerceAtMost(99),
          grenades = (it.grenades + 5).coerceAtMost(20),
          player = it.player.copy(teleportAnimTimer = 0f, invulnerableTimer = 1.6f),
          bannerText = "СЕКТОР ${nextZone + 1}: +1000 ОЧКОВ!",
          bannerTimer = 2.4f,
        )
      }
      loadScreenIntoState(nextZone, 0, resetPlayerPos = true)
    }
  }

  private fun updateParticle(pt: Particle, dt: Float): Particle? {
    val nextLife = pt.life - dt
    if (nextLife <= 0f) return null
    return pt.copy(
      x = pt.x + pt.vx * dt,
      y = pt.y + pt.vy * dt,
      vy = pt.vy + 180f * dt,
      life = nextLife,
    )
  }

  private fun spawnBurst(list: MutableList<Particle>, x: Float, y: Float, colorIdx: Int, count: Int) {
    for (i in 0 until count) {
      val angle = (i.toFloat() / count) * 2f * PI.toFloat() + Random.nextFloat() * 0.4f
      val spd = 25f + Random.nextFloat() * 85f
      val cIdx = if (i % 3 == 0) 7 else if (i % 2 == 0) colorIdx else 3
      list +=
        Particle(
          x = x,
          y = y,
          vx = cos(angle) * spd,
          vy = sin(angle) * spd - 20f,
          colorIndex = cIdx,
          life = 0.35f + Random.nextFloat() * 0.30f,
          maxLife = 0.65f,
          size = 2f + Random.nextFloat() * 1.8f,
        )
    }
  }

  private fun rectsOverlap(x1: Float, y1: Float, w1: Float, h1: Float, x2: Float, y2: Float, w2: Float, h2: Float): Boolean {
    return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2
  }

  private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return kotlin.math.sqrt(dx * dx + dy * dy)
  }

  override fun onCleared() {
    loopJob?.cancel()
    super.onCleared()
  }
}
