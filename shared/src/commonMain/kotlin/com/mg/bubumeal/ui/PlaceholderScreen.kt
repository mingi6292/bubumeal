package com.mg.bubumeal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 실제 화면을 만들기 전까지 쓰는 임시 화면. 제목 하나와 이동 버튼 목록만 있다. */
@Composable
fun PlaceholderScreen(title: String, actions: List<Pair<String, () -> Unit>>) {
    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        actions.forEach { (label, onClick) ->
            Button(onClick = onClick) {
                Text(label)
            }
        }
    }
}
