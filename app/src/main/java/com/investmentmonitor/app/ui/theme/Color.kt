package com.investmentmonitor.app.ui.theme

import androidx.compose.ui.graphics.Color

// Finance-app palette: calm neutrals + a single accent, red/green reserved strictly for
// price direction (spec section 65 - never rely on color alone; text +/- always present too).
val BrandPrimary = Color(0xFF1B2A4A)
val BrandPrimaryLight = Color(0xFF3A4E7A)
val BrandAccent = Color(0xFF00A876)

val PriceUp = Color(0xFFD32F2F) // JP market convention: red = up
val PriceDown = Color(0xFF2E7D32) // JP market convention: green = down
val PriceFlat = Color(0xFF757575)

val LightBackground = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1A1C1E)
val LightOutline = Color(0xFFDDE1E6)

val DarkBackground = Color(0xFF101216)
val DarkSurface = Color(0xFF1B1E24)
val DarkOnSurface = Color(0xFFE3E5E8)
val DarkOutline = Color(0xFF3A3E46)
