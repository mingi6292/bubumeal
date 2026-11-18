package com.mg.bubumeal.ui.issuecode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mg.bubumeal.ui.dateTimeLabel
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueCodeScreen(
    state: IssueCodeUiState,
    onIssue: () -> Unit,
    onBack: () -> Unit,
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("공유 코드 발급") },
                navigationIcon = { TextButton(onClick = onBack) { Text("◀") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.notOwner) {
                Text("공유 코드는 식단표를 만든 사람만 발급할 수 있어요.")
                return@Column
            }
            Text(
                "코드를 상대에게 전달하면 이 식단표에 함께 참여할 수 있어요. 코드는 한 번만 쓸 수 있고 3시간 동안 유효해요.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.isIssuing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            val issued = state.issued
            if (issued == null) {
                Button(onClick = onIssue, enabled = !state.isIssuing) { Text("코드 발급") }
            } else {
                // 길게 눌러 복사할 수 있게 한다. 공유 버튼은 플랫폼 기능이라 나중에 expect/actual로 넣는다.
                SelectionContainer {
                    Text(
                        issued.code,
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text("${dateTimeLabel(issued.expiresAt, timeZone)}까지 사용할 수 있어요.")
                OutlinedButton(onClick = onIssue, enabled = !state.isIssuing) { Text("새 코드 발급") }
                Text(
                    "새 코드를 발급하면 지금 코드는 바로 쓸 수 없게 돼요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
