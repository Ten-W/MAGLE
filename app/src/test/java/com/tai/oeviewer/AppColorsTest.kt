package com.tai.oeviewer

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class AppColorsTest {
    @Test fun presetsAndPreviouslySavedColorsRemainValid() {
        assertEquals("#000000", AppColors.presets.first { it.first == "纯黑" }.second)
        assertEquals("#FFFFFF", AppColors.presets.first { it.first == "纯白" }.second)
        for (value in AppColors.presets.map { it.second } + listOf("#172D60", "#F7CDEA")) {
            assertTrue(AppColors.valid(value))
            assertEquals(value, AppColors.encode(AppColors.seed(value)))
        }
        for (value in listOf("#123", "#FFFFFFFF", "#GGGGGG", "#123456evil")) assertFalse(AppColors.valid(value))
        for (dark in listOf(true, false)) {
            val base = if (dark) darkColorScheme() else lightColorScheme()
            for (value in AppColors.presets.map { it.second } + listOf("#FAECFA", "#182D60")) {
                val colors = AppColors.scheme(base, dark, "#000000", value)
                if (dark) assertEquals(0xff000000.toInt(), colors.background.toArgb())
                val bright = maxOf(colors.primary.luminance(), colors.onPrimary.luminance())
                val dim = minOf(colors.primary.luminance(), colors.onPrimary.luminance())
                assertTrue((bright + .05f) / (dim + .05f) >= 4.5f)
            }
        }
        val white = AppColors.scheme(darkColorScheme(), true, "#000000", "#FFFFFF")
        assertEquals(0xffffffff.toInt(), white.primary.toArgb())
        assertEquals(0xff000000.toInt(), white.onPrimary.toArgb())
        assertEquals(0xffffffff.toInt(), AppColors.scheme(lightColorScheme(), false, "#FFFFFF", "#000000").background.toArgb())
    }
    @Test fun backgroundsAndAccentsAreIndependentAndBlackIsActuallyBlack() {
        for (dark in listOf(true, false)) {
            val base = if (dark) darkColorScheme() else lightColorScheme()
            val blue = AppColors.scheme(base, dark, "hue:215", "hue:215")
            val pink = AppColors.scheme(base, dark, "hue:215", "hue:335")
            val green = AppColors.scheme(base, dark, "hue:145", "hue:215")
            assertEquals(blue.background, pink.background)
            assertEquals(blue.surfaceContainerHigh, pink.surfaceContainerHigh)
            assertNotEquals(blue.primary, pink.primary)
            assertEquals(blue.primary, green.primary)
            val black = AppColors.scheme(base, dark, "black", "hue:335")
            if (dark) { assertEquals(0xff000000.toInt(), black.background.toArgb()); assertEquals(black.background, black.surface) }
            val bright = maxOf(pink.primary.luminance(), pink.onPrimary.luminance())
            val dim = minOf(pink.primary.luminance(), pink.onPrimary.luminance())
            assertTrue((bright + .05f) / (dim + .05f) >= 4.5f)
        }
    }
}
