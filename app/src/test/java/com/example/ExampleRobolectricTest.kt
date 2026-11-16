package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.data.ExolonLevels
import com.example.game.model.HazardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Exolon", appName)
  }

  @Test
  fun `all zones and screens contain valid teleporters`() {
    for (zone in 0 until ExolonLevels.TOTAL_ZONES) {
      for (screen in 0 until ExolonLevels.SCREENS_PER_ZONE) {
        val data = ExolonLevels.getScreen(zone, screen)
        assertTrue("Screen $zone-$screen should have ground tiles", data.tiles.isNotEmpty())
        if (screen == ExolonLevels.SCREENS_PER_ZONE - 1) {
          assertTrue(
            "Final screen of zone $zone must have a Teleporter",
            data.objects.any { it.type == HazardType.TELEPORTER },
          )
        }
      }
    }
  }
}
