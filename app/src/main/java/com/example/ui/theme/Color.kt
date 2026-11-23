package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Royal Luxury Core Palette
val Midnight = Color(0xFF080610)
val RoyalPurple = Color(0xFF31005F)
val ElectricViolet = Color(0xFF8B24FF)
val Lavender = Color(0xFFC7A4FF)
val MetallicGold = Color(0xFFD4AF37)
val ChampagneGold = Color(0xFFF5D88A)
val SilverWhite = Color(0xFFF5F1FF)

// Derived tones strictly for surfaces and shadows
val RoyalSurface = Color(0xFF120A22)
val RoyalSurfaceHigh = Color(0xFF1B0E33)
val RoyalCardSurface = Color(0xE0120A22) // Surface at 88% opacity
val RoyalNavSurface = Color(0xEB080610)  // Midnight at 92% opacity
val GoldShadow = Color(0xFF7A5C0E)
val GoldHighlight = Color(0xFFFFF1C2)
val ErrorColor = Color(0xFFFF6B6B)
val SuccessColor = Color(0xFF3DDC84)
val RoyalErrorContainer = Color(0xD92D0E18)
val RoyalSuccessContainer = Color(0xD9102921)

// Semantic tokens mapped to Royal Luxury palette (keeps existing component references cohesive)
val DarkBackground = Color.Transparent
val DarkSurface = RoyalSurface
val DarkCard = RoyalCardSurface
val DarkSurfaceVariant = RoyalSurfaceHigh
val DarkSurfaceElevated = RoyalSurfaceHigh

val GoldPrimary = MetallicGold
val GoldAccent = ChampagneGold
val GoldLight = ChampagneGold
val GoldDark = GoldShadow
val GoldMuted = GoldShadow

val DarkTextOnGold = Midnight
val EmbossedPurpleOnGold = RoyalPurple

val TextPrimary = SilverWhite
val TextSecondary = Lavender
val TextMuted = Lavender
val TextGold = MetallicGold

val ErrorRed = ErrorColor
val ErrorContainer = RoyalErrorContainer
val SuccessGreen = SuccessColor
val SuccessContainer = RoyalSuccessContainer

val DarkBorder = MetallicGold.copy(alpha = 0.40f)
val DarkBorderHighlight = ChampagneGold.copy(alpha = 0.70f)
