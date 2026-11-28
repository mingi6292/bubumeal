package com.mg.bubumeal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** 부부밀 테마. 화면은 `MaterialTheme.colorScheme/typography/shapes`만 보면 이 값을 받는다. */
@Composable
fun BubuMealTheme(content: @Composable () -> Unit) {
    val family = suitFontFamily()
    val typography = remember(family) { Typography().withFontFamily(family) }
    MaterialTheme(
        colorScheme = BubuMealColorScheme,
        shapes = BubuMealShapes,
        typography = typography,
        content = content,
    )
}
