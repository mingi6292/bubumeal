package com.mg.bubumeal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import bubumeal.shared.generated.resources.Res
import bubumeal.shared.generated.resources.suit_bold
import bubumeal.shared.generated.resources.suit_medium
import bubumeal.shared.generated.resources.suit_regular
import bubumeal.shared.generated.resources.suit_semibold
import org.jetbrains.compose.resources.Font

/** 모든 글자 스타일의 글꼴만 [family]로 바꾼다. 크기와 굵기는 그대로 둔다. */
fun Typography.withFontFamily(family: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

/**
 * SUIT 글꼴(v2.0.5, OFL). Compose 리소스의 [Font]는 플랫폼마다 파일을 읽는 방법이 달라
 * Composable 안에서만 만들 수 있다.
 */
@Composable
fun suitFontFamily(): FontFamily = FontFamily(
    Font(Res.font.suit_regular, FontWeight.Normal),
    Font(Res.font.suit_medium, FontWeight.Medium),
    Font(Res.font.suit_semibold, FontWeight.SemiBold),
    Font(Res.font.suit_bold, FontWeight.Bold),
)
