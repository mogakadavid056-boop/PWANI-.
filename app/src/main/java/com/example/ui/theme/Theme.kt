package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Single font family across the entire app with sans-serif fallback
val PoppinsFamily: FontFamily = try {
  FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.SemiBold),
    Font(R.font.poppins, FontWeight.Bold)
  )
} catch (_: Throwable) {
  FontFamily.SansSerif
}

// Backward-compatible alias so existing references resolve to Poppins
val InterFamily: FontFamily = PoppinsFamily

// Design system type scale strictly matching typography guidelines:
// Screen titles & top bar titles: 24sp, Bold
val TitleStyle = TextStyle(
  fontFamily = PoppinsFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 24.sp,
  lineHeight = 32.sp,
  color = TextPrimary
)

// Section headings (e.g. "Latest Announcements", "Timetable"): 20sp, Bold
val SectionHeadingStyle = TextStyle(
  fontFamily = PoppinsFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 20.sp,
  lineHeight = 28.sp,
  color = TextPrimary
)

// Card titles and list item titles: 18sp, Bold
val CardTitleStyle = TextStyle(
  fontFamily = PoppinsFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 18.sp,
  lineHeight = 24.sp,
  color = TextPrimary
)

// Body text: 16sp, Normal, line height 24sp
val BodyStyle = TextStyle(
  fontFamily = PoppinsFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 16.sp,
  lineHeight = 24.sp,
  color = TextPrimary
)

// Secondary text and captions: 14sp, never smaller than 13sp
val SecondaryTextStyle = TextStyle(
  fontFamily = PoppinsFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp,
  color = TextSecondary
)

val CaptionStyle = TextStyle(
  fontFamily = PoppinsFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp,
  color = TextSecondary
)

// Buttons and chips: 16sp, SemiBold
val ButtonTextStyle = TextStyle(
  fontFamily = PoppinsFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 16.sp,
  lineHeight = 22.sp,
  color = DarkTextOnGold
)

// Material 3 Typography mapping
val AppTypography = Typography(
  displayLarge = TitleStyle.copy(fontSize = 28.sp),
  displayMedium = TitleStyle.copy(fontSize = 26.sp),
  displaySmall = TitleStyle,
  headlineLarge = TitleStyle,
  headlineMedium = SectionHeadingStyle,
  headlineSmall = CardTitleStyle,
  titleLarge = TitleStyle, // 24sp, Bold
  titleMedium = SectionHeadingStyle, // 20sp, Bold
  titleSmall = CardTitleStyle, // 18sp, Bold
  bodyLarge = BodyStyle, // 16sp, Normal, line height 24sp
  bodyMedium = SecondaryTextStyle, // 14sp, Normal
  bodySmall = CaptionStyle, // 14sp
  labelLarge = ButtonTextStyle, // 16sp, SemiBold
  labelMedium = SecondaryTextStyle, // 14sp
  labelSmall = CaptionStyle.copy(fontSize = 13.sp) // 13sp min
)

// Layout & dimension constants
val ScreenPadding = 16.dp
val ItemSpacing = 12.dp
val CardRadius = 16.dp
val ButtonRadius = 12.dp
val ButtonMinHeight = 48.dp

private val DarkColorScheme = darkColorScheme(
  primary = GoldPrimary,
  onPrimary = DarkTextOnGold, // Dark #1A1405 for gold buttons/fab/chips/tabs
  primaryContainer = DarkSurfaceVariant,
  onPrimaryContainer = GoldLight,
  secondary = GoldPrimary,
  onSecondary = DarkTextOnGold,
  secondaryContainer = DarkSurfaceVariant,
  onSecondaryContainer = GoldLight,
  tertiary = GoldAccent,
  onTertiary = DarkTextOnGold,
  background = DarkBackground,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = DarkBorder,
  outlineVariant = DarkBorderHighlight,
  error = ErrorRed,
  onError = TextPrimary,
  errorContainer = ErrorContainer,
  onErrorContainer = ErrorRed
)

@Composable
fun ClassRepTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = AppTypography,
    content = content
  )
}
