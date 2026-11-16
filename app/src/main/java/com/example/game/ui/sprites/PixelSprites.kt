package com.example.game.ui.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.SpectrumBlue
import com.example.ui.theme.SpectrumCyan
import com.example.ui.theme.SpectrumGreen
import com.example.ui.theme.SpectrumMagenta
import com.example.ui.theme.SpectrumOrange
import com.example.ui.theme.SpectrumRed
import com.example.ui.theme.SpectrumVoid
import com.example.ui.theme.SpectrumWhite
import com.example.ui.theme.SpectrumYellow

object PixelSprites {

  // Palette indices for sprites & particles
  val palette =
    arrayOf(
      SpectrumVoid,    // 0: transparent / black
      SpectrumCyan,    // 1: C
      SpectrumMagenta, // 2: M
      SpectrumYellow,  // 3: Y
      SpectrumGreen,   // 4: G
      SpectrumRed,     // 5: R
      SpectrumBlue,    // 6: B
      SpectrumWhite,   // 7: W
      SpectrumOrange,  // 8: O
    )

  fun colorForChar(ch: Char): Color? =
    when (ch) {
      '.' -> null
      'K' -> SpectrumVoid
      'C' -> SpectrumCyan
      'M' -> SpectrumMagenta
      'Y' -> SpectrumYellow
      'G' -> SpectrumGreen
      'R' -> SpectrumRed
      'B' -> SpectrumBlue
      'W' -> SpectrumWhite
      'O' -> SpectrumOrange
      else -> null
    }

  // Standard Space Marine (12x16 sprite grid)
  val marineWalkA =
    listOf(
      "....CCCC....",
      "...CCCCCC...",
      "...CCWWWW...",
      "...CCWWWW...",
      "....CCCC....",
      "..BBBBBBBB..",
      ".BWWWWWWWWB.",
      ".BWCCCCCCWRR",
      ".BWWWWWWWW..",
      "..BBBBBBBB..",
      "...BB..BB...",
      "...BB..BB...",
      "..BBB..BBB..",
      "..CC....CC..",
      ".CCC....CCC.",
      "............",
    )

  val marineWalkB =
    listOf(
      "....CCCC....",
      "...CCCCCC...",
      "...CCWWWW...",
      "...CCWWWW...",
      "....CCCC....",
      "..BBBBBBBB..",
      ".BWWWWWWWWB.",
      ".BWCCCCCCWRR",
      ".BWWWWWWWW..",
      "..BBBBBBBB..",
      "....BBBB....",
      "...BB..BB...",
      "..CC....CC..",
      ".CCC....CC..",
      "............",
      "............",
    )

  val marineDuck =
    listOf(
      "....CCCC....",
      "...CCCCCC...",
      "...CCWWWW...",
      "..BBBBBBBB..",
      ".BWWWWWWWWRR",
      ".BWCCCCCCW..",
      "..BBBBBBBB..",
      "..CCC..CCC..",
      ".CCC....CCC.",
      "............",
    )

  // Heavy Exosuit (14x16 sprite grid)
  val exoWalkA =
    listOf(
      "...YYYYYYYY...",
      "..YYCCCCCCYY..",
      "..YCCWWWWCCY..",
      "..YYYYYYYYYY..",
      ".MMYYYYYYYYMM.",
      "MMWWWWWWWWWWMM",
      "MWWCCCCCCCCWRR",
      "MMWWWWWWWWWWYY",
      ".MMYYYYYYYYMM.",
      "..MMMMMMMMMM..",
      "..MMM....MMM..",
      "..MMM....MMM..",
      ".YYYY....YYYY.",
      ".CCCC....CCCC.",
      "..............",
      "..............",
    )

  val exoWalkB =
    listOf(
      "...YYYYYYYY...",
      "..YYCCCCCCYY..",
      "..YCCWWWWCCY..",
      "..YYYYYYYYYY..",
      ".MMYYYYYYYYMM.",
      "MMWWWWWWWWWWMM",
      "MWWCCCCCCCCWRR",
      "MMWWWWWWWWWWYY",
      ".MMYYYYYYYYMM.",
      "..MMMMMMMMMM..",
      "...MMM..MMM...",
      "..MMM....MMM..",
      ".YYYY....YYYY.",
      "..............",
      "..............",
      "..............",
    )

  val exoDuck =
    listOf(
      "...YYYYYYYY...",
      "..YYCCCCCCYY..",
      "..YCCWWWWCCY..",
      ".MMWWWWWWWWWRR",
      "MMWCCCCCCCCWYY",
      ".MMMMMMMMMMMM.",
      "..YYYY..YYYY..",
      ".CCCC....CCCC.",
      "..............",
    )

  // Turret facing Left (12x12)
  val turretLeft =
    listOf(
      "....MMMM....",
      "..MMMMMMMM..",
      "RRRRWWWWWWMM",
      "RRRRWWWWWWMM",
      "..MMMMMMMM..",
      "...CCCCCC...",
      "..CCWWWWCC..",
      ".CCCCCCCCCC.",
      ".CMMMMMMMMC.",
      "CCCCCCCCCCCC",
    )

  // Heavy Cannon facing Left (14x12)
  val heavyCannon =
    listOf(
      ".....YYYYYY...",
      "...YYYYYYYYYY.",
      "RRRRRWWWWWWWWY",
      ".....MMMMMMMM.",
      "RRRRRWWWWWWWWY",
      "...YYYYYYYYYY.",
      "..BBBBBBBBBBBB",
      ".BCCCCCCCCCCCB",
      ".BRRRRRRRRRRRB",
      "BBBBBBBBBBBBBB",
    )

  // Land Mine (10x6)
  val mineSprite =
    listOf(
      "....RR....",
      "..R.YY.R..",
      ".RRRRRRRR.",
      "MMMMMMMMMM",
      "CCCCCCCCCC",
    )

  // Rocket Silo (12x12)
  val rocketSilo =
    listOf(
      "....WWWW....",
      "...WYYYYW...",
      "...WRRRRW...",
      "..CCCCCCCC..",
      ".CCMMMMMMCC.",
      ".CMWWWWWWMC.",
      ".CMWWWWWWMC.",
      ".CCCCCCCCCC.",
      "BBBBBBBBBBBB",
    )

  // Alien Sine Eye Drone (12x10)
  val droneEye =
    listOf(
      "...GGGGGG...",
      ".GGGGGGGGGG.",
      "GGWWWRRWWWGG",
      "GGWWRRRRWWGG",
      "GGWWWRRWWWGG",
      ".GGGGGGGGGG.",
      "...GG..GG...",
      "..YY....YY..",
    )

  // Alien Swoop Hornet (12x10)
  val droneHornet =
    listOf(
      "..MM....MM..",
      ".MMMM..MMMM.",
      "..MMMMMMMM..",
      "RRYYYYYYYYMM",
      ".RYYYYYYYY..",
      "..MMMMMMMM..",
      "...CC..CC...",
    )

  // Alien Bounce Orb (10x10)
  val droneOrb =
    listOf(
      "...CCCC...",
      ".CCCCCCCC.",
      "CCWWMMWWCC",
      "CCWMMMMWCC",
      "CCWWMMWWCC",
      ".CCCCCCCC.",
      "...CCCC...",
    )

  // Pickups (10x10)
  val pickupAmmo =
    listOf(
      ".CCCCCCCC.",
      "CWWWWWWWWC",
      "CWCCCCCCWC",
      "CWCYYYYCWC",
      "CWCYYYYCWC",
      "CWCCCCCCWC",
      "CWWWWWWWWC",
      ".CCCCCCCC.",
    )

  val pickupGrenade =
    listOf(
      "...YYYY...",
      "....YY....",
      "..GGGGGG..",
      ".GYYYYYYG.",
      ".GGGGGGGG.",
      ".GYYYYYYG.",
      "..GGGGGG..",
    )

  val pickupLife =
    listOf(
      ".RR....RR.",
      "RRRR..RRRR",
      "RRRRRRRRRR",
      "RRWWRRRRRR",
      ".RRRRRRRR.",
      "..RRRRRR..",
      "...RRRR...",
      "....RR....",
    )

  val pickupCrystal =
    listOf(
      "....CC....",
      "...CWWC...",
      "..CWMMWC..",
      ".CWMMMMWC.",
      "..CWMMWC..",
      "...CWWC...",
      "....CC....",
    )
}

fun DrawScope.drawPixelSprite(
  rows: List<String>,
  x: Float,
  y: Float,
  targetW: Float,
  targetH: Float,
  flipX: Boolean = false,
  tintOverride: Color? = null,
) {
  if (rows.isEmpty()) return
  val gridH = rows.size
  val gridW = rows[0].length
  val pxW = targetW / gridW
  val pxH = targetH / gridH

  for (r in 0 until gridH) {
    val line = rows[r]
    for (c in 0 until line.length) {
      val ch = line[c]
      val color = PixelSprites.colorForChar(ch) ?: continue
      val drawCol = if (flipX) (gridW - 1 - c) else c
      drawRect(
        color = tintOverride ?: color,
        topLeft = Offset(x + drawCol * pxW, y + r * pxH),
        size = Size(pxW + 0.35f, pxH + 0.35f),
      )
    }
  }
}
