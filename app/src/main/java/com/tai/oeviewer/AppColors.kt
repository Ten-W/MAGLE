package com.tai.oeviewer

import android.annotation.SuppressLint
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeMonochrome

// MCU ships inside the pinned Material artifact without a standalone Google Java artifact.
// Keep the restricted dependency isolated here; AppColorsTest checks its color/contrast contract.
@SuppressLint("RestrictedApi")
internal object AppColors {
    val presets = listOf("纯黑" to "#000000", "纯白" to "#FFFFFF", "中性灰" to "#808080", "红色" to "#C95858",
        "橙色" to "#CF813C", "黄色" to "#C4A238", "绿色" to "#438966", "青色" to "#398F9A",
        "蓝色" to "#4278C4", "靛蓝" to "#5D63B8", "紫色" to "#9365B8", "粉色" to "#C76E98")
    fun hue(value: String): Float? = when (value) {
        "blue" -> 215f; "green" -> 145f; "purple" -> 275f; "orange" -> 35f; "pink" -> 335f
        else -> value.removePrefix("hue:").toFloatOrNull()?.takeIf { it in 0f..359f }
    }
    fun valid(value: String) = value in setOf("wallpaper", "black", "white") || value.matches(Regex("#[0-9a-fA-F]{6}")) || hue(value) != null
    fun seed(value: String): Color = when {
        value == "black" -> Color.Black
        value == "white" -> Color.White
        value.matches(Regex("#[0-9a-fA-F]{6}")) -> Color((0xff000000L or value.drop(1).toLong(16)).toInt())
        else -> Color.hsl(hue(value) ?: 215f, .7f, .5f)
    }
    fun encode(color: Color) = "#" + (color.toArgb() and 0xffffff).toString(16).padStart(6, '0').uppercase(java.util.Locale.ROOT)
    private fun neutral(value: String): Boolean {
        val color = seed(value)
        return value != "wallpaper" && maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue) <= .01f
    }
    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)

    fun scheme(base: ColorScheme, dark: Boolean, theme: String, accent: String): ColorScheme {
        var colors = base
        if (theme != "wallpaper" && !neutral(theme)) {
            val s = SchemeTonalSpot(Hct.fromInt(seed(theme).toArgb()), dark, 0.0)
            colors = colors.copy(background = Color(s.background), onBackground = Color(s.onBackground),
                surface = Color(s.surface), onSurface = Color(s.onSurface), surfaceDim = Color(s.surfaceDim), surfaceBright = Color(s.surfaceBright),
                surfaceVariant = Color(s.surfaceVariant), onSurfaceVariant = Color(s.onSurfaceVariant),
                surfaceContainerLowest = Color(s.surfaceContainerLowest), surfaceContainerLow = Color(s.surfaceContainerLow),
                surfaceContainer = Color(s.surfaceContainer), surfaceContainerHigh = Color(s.surfaceContainerHigh), surfaceContainerHighest = Color(s.surfaceContainerHighest),
                inverseSurface = Color(s.inverseSurface), inverseOnSurface = Color(s.inverseOnSurface), outline = Color(s.outline), outlineVariant = Color(s.outlineVariant))
        }
        if (theme != "wallpaper" && neutral(theme)) {
            fun gray(tone: Double) = Color(Hct.from(0.0, 0.0, tone).toInt())
            val chosenTone = Hct.fromInt(seed(theme).toArgb()).tone
            val backgroundTone = if (dark) minOf(chosenTone, 12.0) else maxOf(chosenTone, 90.0)
            colors = colors.copy(background = gray(backgroundTone), onBackground = gray(if (dark) 90.0 else 10.0),
                surface = gray(backgroundTone), onSurface = gray(if (dark) 90.0 else 10.0),
                surfaceDim = gray(if (dark) 0.0 else 87.0), surfaceBright = gray(if (dark) 24.0 else 98.0),
                surfaceContainerLowest = gray(if (dark) 0.0 else 100.0), surfaceContainerLow = gray(if (dark) 6.0 else 96.0),
                surfaceContainer = gray(if (dark) 10.0 else 94.0), surfaceContainerHigh = gray(if (dark) 15.0 else 92.0), surfaceContainerHighest = gray(if (dark) 20.0 else 90.0),
                surfaceVariant = gray(if (dark) 24.0 else 90.0), onSurfaceVariant = gray(if (dark) 80.0 else 30.0),
                outline = gray(if (dark) 60.0 else 50.0), outlineVariant = gray(if (dark) 30.0 else 80.0))
        }
        if (accent != "wallpaper") {
            val hct = Hct.fromInt(seed(accent).toArgb())
            val s = if (neutral(accent)) SchemeMonochrome(hct, dark, 0.0) else SchemeTonalSpot(hct, dark, 0.0)
            val primary = if (accent.startsWith('#') && contrast(seed(accent), colors.background) >= 3f) seed(accent) else Color(s.primary)
            val onPrimary = if (contrast(primary, Color.Black) >= contrast(primary, Color.White)) Color.Black else Color.White
            colors = colors.copy(primary = primary, onPrimary = onPrimary, primaryContainer = Color(s.primaryContainer), onPrimaryContainer = Color(s.onPrimaryContainer), inversePrimary = Color(s.inversePrimary),
                secondary = Color(s.secondary), onSecondary = Color(s.onSecondary), secondaryContainer = Color(s.secondaryContainer), onSecondaryContainer = Color(s.onSecondaryContainer),
                tertiary = Color(s.tertiary), onTertiary = Color(s.onTertiary), tertiaryContainer = Color(s.tertiaryContainer), onTertiaryContainer = Color(s.onTertiaryContainer))
        }
        return colors.copy(surfaceTint = colors.primary)
    }
}
