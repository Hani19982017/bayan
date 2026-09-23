package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * App Theme Mode (Light / Dark / System)
 */
enum class AppThemeMode(val titleAr: String, val subtitleAr: String) {
    LIGHT("الوضع الفاتح (نهاري)", "مظهر ساطع ونقي بإضاءة واضحة ومتباينة"),
    DARK("الوضع الداكن (ليلي)", "مظهر داكن مريح للعينين وموفر لاستهلاك البطارية"),
    SYSTEM("تلقائي (حسب النظام)", "يتكيف تلقائياً مع نمط المظهر المختار في جهازك")
}

private val DarkColorScheme =
  darkColorScheme(
    primary = TawthiqPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF38BDF8),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TawthiqPrimary,
    onPrimary = Color.White,
    primaryContainer = TawthiqPrimaryContainer,
    onPrimaryContainer = TawthiqOnPrimaryContainer,
    secondary = TawthiqSecondary,
    secondaryContainer = TawthiqSecondaryContainer,
    tertiary = TawthiqAmber,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our brand colors for cohesive Tawthiq styling
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

