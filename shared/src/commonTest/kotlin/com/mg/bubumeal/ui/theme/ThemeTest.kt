package com.mg.bubumeal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeTest {

    @Test
    fun colorScheme_usesConfirmedColors() {
        with(BubuMealColorScheme) {
            assertEquals(Color(0xFFFAF4EA), background)
            assertEquals(Color(0xFFFAF4EA), surface)
            assertEquals(Color(0xFFFFFDF8), surfaceContainerLowest)
            assertEquals(Color(0xFFFFFDF8), surfaceContainerHighest)
            assertEquals(Color(0xFF3D2F25), onSurface)
            assertEquals(Color(0xFF8A7565), onSurfaceVariant)
            assertEquals(Color(0xFFEADFCE), outlineVariant)
            assertEquals(Color(0xFFA8937C), outline)
            assertEquals(Color(0xFFC4552D), primary)
            assertEquals(Color.White, onPrimary)
            assertEquals(primary, secondary)
            assertEquals(primary, tertiary)
            assertEquals(Color(0xFFF7E1D6), primaryContainer)
        }
    }

    @Test
    fun shapes_useAgreedRadii() {
        assertEquals(RoundedCornerShape(20.dp), BubuMealShapes.large)
        assertEquals(RoundedCornerShape(12.dp), BubuMealShapes.small)
    }

    @Test
    fun withFontFamily_appliesToEveryStyle() {
        val family = FontFamily.Serif
        val t = Typography().withFontFamily(family)
        listOf(
            t.displayLarge, t.displayMedium, t.displaySmall,
            t.headlineLarge, t.headlineMedium, t.headlineSmall,
            t.titleLarge, t.titleMedium, t.titleSmall,
            t.bodyLarge, t.bodyMedium, t.bodySmall,
            t.labelLarge, t.labelMedium, t.labelSmall,
        ).forEach { assertEquals(family, it.fontFamily) }
        assertEquals(Typography().bodyLarge.fontSize, t.bodyLarge.fontSize)
    }
}
