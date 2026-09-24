package com.nhowe.ember.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val EmberOrange = Color(0xFFFF6B35)
val EmberOrangeDeep = Color(0xFFE04E1B)
val Gold = Color(0xFFFFC145)
val Violet = Color(0xFF8B5CF6)
val Mint = Color(0xFF34D399)
val Ice = Color(0xFF7DD3FC)
val Rose = Color(0xFFF43F5E)

// Dark surfaces
val Night = Color(0xFF0E0F14)
val NightSurface = Color(0xFF171922)
val NightSurfaceHigh = Color(0xFF1F2230)
val NightSurfaceHighest = Color(0xFF2A2E3F)
val NightOutline = Color(0xFF3A3F55)
val NightText = Color(0xFFF3F4F8)
val NightTextMuted = Color(0xFF9AA0B5)

// Light surfaces
val Day = Color(0xFFFBF7F2)
val DaySurface = Color(0xFFFFFFFF)
val DaySurfaceHigh = Color(0xFFF3EEE7)
val DaySurfaceHighest = Color(0xFFEAE3DA)
val DayOutline = Color(0xFFD9D2C7)
val DayText = Color(0xFF1B1A22)
val DayTextMuted = Color(0xFF6B6A78)

/** Curated swatches users pick for their goals. Index is stored, not the color. */
val GoalPalette: List<Color> = listOf(
    Color(0xFFFF6B35), // ember
    Color(0xFFFFC145), // gold
    Color(0xFF34D399), // mint
    Color(0xFF38BDF8), // sky
    Color(0xFF8B5CF6), // violet
    Color(0xFFF472B6), // pink
    Color(0xFFF43F5E), // rose
    Color(0xFFA3E635), // lime
    Color(0xFF2DD4BF), // teal
    Color(0xFFFB923C), // tangerine
)

fun goalColor(index: Int): Color = GoalPalette[index.mod(GoalPalette.size)]
