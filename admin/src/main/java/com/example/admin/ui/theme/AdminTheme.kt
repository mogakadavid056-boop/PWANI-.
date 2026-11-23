package com.example.admin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.R

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

// Semantic tokens mapped cleanly to Royal Luxury Palette
val ObsidianBgTop = Midnight
val ObsidianBgMid = Midnight
val ObsidianBgBottom = RoyalPurple
val AdminSurface = RoyalSurface
val AdminCard = RoyalCardSurface
val AdminCardElevated = RoyalSurfaceHigh

val GoldPrimary = MetallicGold
val GoldLight = ChampagneGold
val GoldDark = GoldShadow
val DarkTextOnGold = Midnight
val EmbossedPurpleOnGold = RoyalPurple

val TextPrimary = SilverWhite
val TextSecondary = Lavender
val TextMuted = Lavender.copy(alpha = 0.80f)

val EmeraldSuccess = SuccessColor
val EmeraldContainer = RoyalSuccessContainer
val AmberWarning = ChampagneGold
val AmberContainer = RoyalSurfaceHigh
val CrimsonError = ErrorColor
val CrimsonContainer = RoyalErrorContainer
val InfoBlue = ElectricViolet

val AdminBorder = MetallicGold.copy(alpha = 0.40f)
val AdminBorderStrong = MetallicGold

// Bundled local Google Font families with Serif/SansSerif fallbacks
val CinzelDecorativeFamily: FontFamily = try {
  FontFamily(
    Font(R.font.cinzel_decorative, FontWeight.Bold)
  )
} catch (_: Throwable) {
  FontFamily.Serif
}

val PlayfairDisplayFamily: FontFamily = try {
  FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold),
    Font(R.font.playfair_display, FontWeight.Normal, FontStyle.Italic)
  )
} catch (_: Throwable) {
  FontFamily.Serif
}

val GreatVibesFamily: FontFamily = try {
  FontFamily(
    Font(R.font.great_vibes, FontWeight.Normal)
  )
} catch (_: Throwable) {
  FontFamily.Cursive
}

val MontserratFamily: FontFamily = try {
  FontFamily(
    Font(R.font.montserrat, FontWeight.Medium),
    Font(R.font.montserrat, FontWeight.SemiBold),
    Font(R.font.montserrat, FontWeight.Bold)
  )
} catch (_: Throwable) {
  FontFamily.SansSerif
}

val InterFamily: FontFamily = try {
  FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold)
  )
} catch (_: Throwable) {
  FontFamily.SansSerif
}

// Legacy alias kept for source compatibility -> Inter
val AdminFontFamily: FontFamily = InterFamily

// Royal Luxury Admin Type Scale
val AdminWordmarkStyle = TextStyle(
  fontFamily = CinzelDecorativeFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 36.sp,
  lineHeight = 42.sp,
  letterSpacing = 1.5.sp,
  color = ChampagneGold
)

val AdminScreenTitleStyle = TextStyle(
  fontFamily = CinzelDecorativeFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 24.sp,
  lineHeight = 32.sp,
  letterSpacing = 1.5.sp,
  color = ChampagneGold
)

val AdminSectionHeadingStyle = TextStyle(
  fontFamily = PlayfairDisplayFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 20.sp,
  lineHeight = 28.sp,
  color = ChampagneGold
)

val AdminCardTitleStyle = TextStyle(
  fontFamily = PlayfairDisplayFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 18.sp,
  lineHeight = 24.sp,
  color = SilverWhite
)

val AdminSubtitleItalicStyle = TextStyle(
  fontFamily = PlayfairDisplayFamily,
  fontWeight = FontWeight.Normal,
  fontStyle = FontStyle.Italic,
  fontSize = 16.sp,
  lineHeight = 22.sp,
  color = Lavender
)

val AdminGreetingStyle = TextStyle(
  fontFamily = GreatVibesFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 30.sp,
  lineHeight = 36.sp,
  color = ChampagneGold
)

val AdminButtonTextStyle = TextStyle(
  fontFamily = MontserratFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 16.sp,
  lineHeight = 22.sp
)

val AdminFieldLabelStyle = TextStyle(
  fontFamily = MontserratFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 14.sp,
  lineHeight = 20.sp,
  color = ChampagneGold
)

val AdminNavLabelStyle = TextStyle(
  fontFamily = MontserratFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 12.sp,
  lineHeight = 16.sp
)

val AdminBodyStyle = TextStyle(
  fontFamily = InterFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 16.sp,
  lineHeight = 24.sp,
  color = SilverWhite
)

val AdminCaptionStyle = TextStyle(
  fontFamily = InterFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp,
  color = Lavender
)

private val AdminTypography = Typography(
  displayLarge = AdminWordmarkStyle,
  displayMedium = AdminScreenTitleStyle.copy(fontSize = 28.sp, lineHeight = 34.sp),
  displaySmall = AdminScreenTitleStyle,
  headlineLarge = AdminScreenTitleStyle,
  headlineMedium = AdminSectionHeadingStyle,
  headlineSmall = AdminCardTitleStyle,
  titleLarge = AdminScreenTitleStyle,
  titleMedium = AdminSectionHeadingStyle,
  titleSmall = AdminCardTitleStyle,
  bodyLarge = AdminBodyStyle,
  bodyMedium = AdminCaptionStyle,
  bodySmall = AdminCaptionStyle,
  labelLarge = AdminButtonTextStyle,
  labelMedium = AdminFieldLabelStyle,
  labelSmall = AdminNavLabelStyle
)

val AdminShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(18.dp),
  extraLarge = RoundedCornerShape(24.dp)
)

private val AdminColorScheme = darkColorScheme(
  primary = MetallicGold,
  onPrimary = Midnight,
  primaryContainer = RoyalSurfaceHigh,
  onPrimaryContainer = ChampagneGold,
  secondary = ElectricViolet,
  onSecondary = SilverWhite,
  secondaryContainer = RoyalSurfaceHigh,
  onSecondaryContainer = Lavender,
  tertiary = ChampagneGold,
  onTertiary = Midnight,
  background = Midnight,
  onBackground = SilverWhite,
  surface = RoyalSurface,
  onSurface = SilverWhite,
  surfaceVariant = RoyalSurfaceHigh,
  onSurfaceVariant = Lavender,
  outline = AdminBorder,
  outlineVariant = ChampagneGold.copy(alpha = 0.70f),
  error = ErrorColor,
  onError = Midnight,
  errorContainer = RoyalErrorContainer,
  onErrorContainer = ErrorColor
)

@Composable
fun ClassRepAdminTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = AdminColorScheme,
    typography = AdminTypography,
    shapes = AdminShapes,
    content = content
  )
}
