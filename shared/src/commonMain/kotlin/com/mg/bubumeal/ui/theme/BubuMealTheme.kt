package com.mg.bubumeal.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
    ) {
        // Scaffold를 쓰지 않는 화면(로그인, 시작)도 크림 배경과 글자색을 받도록 앱 전체 바닥에 깐다.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
    }
}
