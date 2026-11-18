package com.example.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.admin.R

val ObsidianBgTop = Color(0xFF05130E)
val ObsidianBgMid = Color(0xFF092219)
val ObsidianBgBottom = Color(0xFF0D2E22)
val AdminSurface = Color(0xFF102A20)
val AdminCard = Color(0xE6113025)
val AdminCardElevated = Color(0xF5153B2D)

val GoldPrimary = Color(0xFFE6B422)
val GoldLight = Color(0xFFF7D97B)
val GoldDark = Color(0xFFB88E14)
val DarkTextOnGold = Color(0xFF1A1405)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFD2DDD7)
val TextMuted = Color(0xFF93A89E)

val EmeraldSuccess = Color(0xFF3DDC84)
val EmeraldContainer = Color(0xD9103824)
val AmberWarning = Color(0xFFFFB74D)
val AmberContainer = Color(0xD93B2A12)
val CrimsonError = Color(0xFFFF6B6B)
val CrimsonContainer = Color(0xD93B1618)
val InfoBlue = Color(0xFF64B5F6)

val AdminBorder = Color(0x47E6B422)
val AdminBorderStrong = Color(0x8CE6B422)

val AdminFontFamily: FontFamily = try {
  FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.SemiBold),
    Font(R.font.poppins, FontWeight.Bold)
  )
} catch (_: Throwable) {
  FontFamily.SansSerif
}

private val AdminTypography = Typography(
  headlineLarge = TextStyle(fontFamily = AdminFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp, color = TextPrimary),
  headlineMedium = TextStyle(fontFamily = AdminFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp, color = TextPrimary),
  titleLarge = TextStyle(fontFamily = AdminFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp, color = TextPrimary),
  titleMedium = TextStyle(fontFamily = AdminFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp, color = TextPrimary),
  titleSmall = TextStyle(fontFamily = AdminFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp, color = TextPrimary),
  bodyLarge = TextStyle(fontFamily = AdminFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, color = TextPrimary),
  bodyMedium = TextStyle(fontFamily = AdminFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = TextSecondary),
  bodySmall = TextStyle(fontFamily = AdminFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = TextMuted),
  labelLarge = TextStyle(fontFamily = AdminFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
)

private val AdminColorScheme = darkColorScheme(
  primary = GoldPrimary,
  onPrimary = DarkTextOnGold,
  primaryContainer = AdminCardElevated,
  onPrimaryContainer = GoldLight,
  secondary = EmeraldSuccess,
  onSecondary = DarkTextOnGold,
  background = ObsidianBgMid,
  onBackground = TextPrimary,
  surface = AdminSurface,
  onSurface = TextPrimary,
  surfaceVariant = AdminCard,
  onSurfaceVariant = TextSecondary,
  outline = AdminBorder,
  error = CrimsonError,
  onError = TextPrimary,
  errorContainer = CrimsonContainer,
  onErrorContainer = CrimsonError
)

@Composable
fun ClassRepAdminTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = AdminColorScheme,
    typography = AdminTypography,
    content = content
  )
}
