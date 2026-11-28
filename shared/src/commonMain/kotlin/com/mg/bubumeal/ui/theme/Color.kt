package com.mg.bubumeal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// "따뜻한 식탁" 색. 밝은 테마만 쓴다(다크 테마 없음).
// 확정 6색: 배경, 카드, 글자, 보조 글자, 선, 강조. 나머지는 설계 문서 3장의 제안 값.
private val Background = Color(0xFFFAF4EA)
private val Card = Color(0xFFFFFDF8)
private val Ink = Color(0xFF3D2F25)
private val SubInk = Color(0xFF8A7565)
private val Line = Color(0xFFEADFCE)
private val Accent = Color(0xFFC4552D)

private val Faint = Color(0xFFA8937C)
private val AccentContainer = Color(0xFFF7E1D6)
private val DeeperCream = Color(0xFFF3EBDE)

val BubuMealColorScheme: ColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentContainer,
    onPrimaryContainer = Ink,
    inversePrimary = AccentContainer,
    // 보조 강조색 자리에도 같은 강조색을 넣어 Material 기본 보라색이 나오지 않게 한다.
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = AccentContainer,
    onSecondaryContainer = Ink,
    tertiary = Accent,
    onTertiary = Color.White,
    tertiaryContainer = AccentContainer,
    onTertiaryContainer = Ink,
    background = Background,
    onBackground = Ink,
    surface = Background,
    onSurface = Ink,
    surfaceVariant = DeeperCream,
    onSurfaceVariant = SubInk,
    surfaceTint = Accent,
    outline = Faint,
    outlineVariant = Line,
    surfaceBright = Card,
    surfaceDim = DeeperCream,
    // 대화상자, 카드, 메뉴 등 떠 있는 면은 모두 카드 색.
    surfaceContainerLowest = Card,
    surfaceContainerLow = Card,
    surfaceContainer = Card,
    surfaceContainerHigh = Card,
    surfaceContainerHighest = Card,
)
