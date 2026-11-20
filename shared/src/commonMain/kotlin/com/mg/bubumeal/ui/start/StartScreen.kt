package com.mg.bubumeal.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** 볼 식단표가 하나도 없을 때의 시작 화면 (docs/screen-flow.md 2장). */
@Composable
fun StartScreen(
    state: StartUiState,
    onCreate: () -> Unit,
    onEnterCode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("부부밀", style = MaterialTheme.typography.headlineLarge)
        Text(
            "아직 볼 수 있는 식단표가 없어요.\n내 식단표를 만들거나, 받은 공유 코드를 입력해 참여해 보세요.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        val busy = state.isCreating || state.goToWeekly
        Button(onClick = onCreate, enabled = !busy) { Text("식단표 만들기") }
        OutlinedButton(onClick = onEnterCode, enabled = !busy) { Text("공유 코드 입력") }
        if (state.isCreating) {
            CircularProgressIndicator()
        }
        if (state.alreadyOwned) {
            Text("이미 내 식단표가 있어요. 한 사람은 식단표를 하나만 만들 수 있어요.", color = MaterialTheme.colorScheme.error)
        }
    }
}
