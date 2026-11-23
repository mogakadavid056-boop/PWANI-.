package com.example.ui.theme

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
import com.example.R

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

// Royal Luxury Type Scale
// Wordmark: 40-44sp Cinzel Decorative Bold
val WordmarkStyle = TextStyle(
  fontFamily = CinzelDecorativeFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 42.sp,
  lineHeight = 48.sp,
  letterSpacing = 1.5.sp,
  color = ChampagneGold
)

// Top-level screen titles (<= 3 words): 24sp Cinzel Decorative Bold, 1.5sp letter spacing
val ScreenTitleStyle = TextStyle(
  fontFamily = CinzelDecorativeFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 24.sp,
  lineHeight = 32.sp,
  letterSpacing = 1.5.sp,
  color = ChampagneGold
)

// Backward-compatible alias for screen titles
val TitleStyle = ScreenTitleStyle

// Section headings: 20sp Playfair Bold
val SectionHeadingStyle = TextStyle(
  fontFamily = PlayfairDisplayFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 20.sp,
  lineHeight = 28.sp,
  color = ChampagneGold
)

// Card titles: 18sp Playfair SemiBold
val CardTitleStyle = TextStyle(
  fontFamily = PlayfairDisplayFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 18.sp,
  lineHeight = 24.sp,
  color = SilverWhite
)

// Profile names: Playfair Bold
val ProfileNameStyle = TextStyle(
  fontFamily = PlayfairDisplayFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 24.sp,
  lineHeight = 30.sp,
  color = SilverWhite
)

// Subtitles: Playfair Italic
val SubtitleItalicStyle = TextStyle(
  fontFamily = PlayfairDisplayFamily,
  fontWeight = FontWeight.Normal,
  fontStyle = FontStyle.Italic,
  fontSize = 16.sp,
  lineHeight = 22.sp,
  color = Lavender
)

// Decorative greetings & taglines: Great Vibes, never below 28sp (32sp default)
val GreetingStyle = TextStyle(
  fontFamily = GreatVibesFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 32.sp,
  lineHeight = 38.sp,
  color = ChampagneGold
)

// Buttons, tabs, chips: 16sp Montserrat SemiBold
val ButtonTextStyle = TextStyle(
  fontFamily = MontserratFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 16.sp,
  lineHeight = 22.sp,
  color = Color.Unspecified
)

// Field labels, chips, badges, support footer: Montserrat SemiBold
val FieldLabelStyle = TextStyle(
  fontFamily = MontserratFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 14.sp,
  lineHeight = 20.sp,
  color = ChampagneGold
)

// Bottom navigation labels: 12sp Montserrat SemiBold
val NavLabelStyle = TextStyle(
  fontFamily = MontserratFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 12.sp,
  lineHeight = 16.sp
)

// Body text: 16sp Inter with 24sp line height
val BodyStyle = TextStyle(
  fontFamily = InterFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 16.sp,
  lineHeight = 24.sp,
  color = SilverWhite
)

// Secondary text and captions: 14sp Inter (never smaller)
val SecondaryTextStyle = TextStyle(
  fontFamily = InterFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp,
  color = Lavender
)

val CaptionStyle = TextStyle(
  fontFamily = InterFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp,
  color = Lavender
)

// Material 3 Typography mapping so Text picks the right font automatically
val AppTypography = Typography(
  displayLarge = WordmarkStyle,
  displayMedium = ScreenTitleStyle.copy(fontSize = 28.sp, lineHeight = 34.sp),
  displaySmall = ScreenTitleStyle,
  headlineLarge = ScreenTitleStyle,
  headlineMedium = SectionHeadingStyle,
  headlineSmall = CardTitleStyle,
  titleLarge = ScreenTitleStyle,
  titleMedium = SectionHeadingStyle,
  titleSmall = CardTitleStyle,
  bodyLarge = BodyStyle,
  bodyMedium = SecondaryTextStyle,
  bodySmall = CaptionStyle,
  labelLarge = ButtonTextStyle,
  labelMedium = FieldLabelStyle,
  labelSmall = NavLabelStyle
)

val RoyalShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(18.dp),
  extraLarge = RoundedCornerShape(24.dp)
)

// Layout & dimension constants
val ScreenPadding = 16.dp
val ItemSpacing = 12.dp
val CardRadius = 18.dp
val ButtonRadius = 14.dp
val ButtonMinHeight = 52.dp

private val DarkColorScheme = darkColorScheme(
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
  background = DarkBackground,
  onBackground = SilverWhite,
  surface = RoyalSurface,
  onSurface = SilverWhite,
  surfaceVariant = RoyalSurfaceHigh,
  onSurfaceVariant = Lavender,
  outline = DarkBorder,
  outlineVariant = DarkBorderHighlight,
  error = ErrorColor,
  onError = Midnight,
  errorContainer = RoyalErrorContainer,
  onErrorContainer = ErrorColor
)

@Composable
fun ClassRepTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = AppTypography,
    shapes = RoyalShapes,
    content = content
  )
}
