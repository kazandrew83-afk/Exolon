package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Pure Kotlin 8-bit ZX Spectrum Beeper / AY-3-8912 inspired procedural SFX synthesizer.
 * Zero external audio assets required.
 */
class RetroAudioEngine {
  private val sampleRate = 22050
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @Volatile var enabled: Boolean = true

  fun playLaser(isTwin: Boolean = false) {
    if (!enabled) return
    playPcmAsync(durationMs = 85) { i, total ->
      val t = i.toFloat() / total
      val freq = if (isTwin) 1450f - 1050f * t else 1200f - 850f * t
      squareWave(i, freq) * (1f - t) * 0.25f
    }
  }

  fun playGrenadeThrow() {
    if (!enabled) return
    playPcmAsync(durationMs = 110) { i, total ->
      val t = i.toFloat() / total
      val freq = 320f + 260f * sin(t * PI.toFloat())
      squareWave(i, freq) * (1f - t * 0.6f) * 0.22f
    }
  }

  fun playExplosion(heavy: Boolean = false) {
    if (!enabled) return
    val ms = if (heavy) 280 else 165
    playPcmAsync(durationMs = ms) { i, total ->
      val t = i.toFloat() / total
      val noise = Random.nextFloat() * 2f - 1f
      val rumble = squareWave(i, 95f - 55f * t)
      ((noise * 0.65f + rumble * 0.35f) * (1f - t) * 0.38f)
    }
  }

  fun playJump() {
    if (!enabled) return
    playPcmAsync(durationMs = 95) { i, total ->
      val t = i.toFloat() / total
      val freq = 260f + 520f * t
      squareWave(i, freq) * (1f - t * 0.4f) * 0.20f
    }
  }

  fun playPickup() {
    if (!enabled) return
    playPcmAsync(durationMs = 140) { i, total ->
      val t = i.toFloat() / total
      val freq =
        when {
          t < 0.33f -> 660f
          t < 0.66f -> 880f
          else -> 1320f
        }
      squareWave(i, freq) * 0.22f
    }
  }

  fun playExoPowerUp() {
    if (!enabled) return
    playPcmAsync(durationMs = 340) { i, total ->
      val t = i.toFloat() / total
      val step = (t * 6f).toInt().coerceAtMost(5)
      val freqs = floatArrayOf(330f, 440f, 554f, 659f, 880f, 1108f)
      squareWave(i, freqs[step]) * (1f - t * 0.2f) * 0.28f
    }
  }

  fun playTeleport() {
    if (!enabled) return
    playPcmAsync(durationMs = 420) { i, total ->
      val t = i.toFloat() / total
      val warble = sin(t * 28f) * 240f
      val freq = 420f + 900f * t + warble
      squareWave(i, freq) * (1f - t * 0.15f) * 0.26f
    }
  }

  fun playPlayerHit() {
    if (!enabled) return
    playPcmAsync(durationMs = 260) { i, total ->
      val t = i.toFloat() / total
      val noise = Random.nextFloat() * 2f - 1f
      val drop = squareWave(i, 380f - 310f * t)
      (noise * 0.45f + drop * 0.55f) * (1f - t) * 0.36f
    }
  }

  private fun squareWave(sampleIndex: Int, freqHz: Float): Float {
    val safeFreq = freqHz.coerceAtLeast(30f)
    val period = sampleRate / safeFreq
    val phase = (sampleIndex % period) / period
    return if (phase < 0.5f) 1f else -1f
  }

  private fun playPcmAsync(durationMs: Int, generator: (index: Int, total: Int) -> Float) {
    scope.launch {
      runCatching {
        val totalSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
          val sample = generator(i, totalSamples).coerceIn(-1f, 1f)
          buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        val minBuf =
          AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
        val track =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, buffer.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        kotlinx.coroutines.delay(durationMs.toLong() + 40L)
        track.stop()
        track.release()
      }
    }
  }
}
