package com.mg.bubumeal.ui.plansettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mg.bubumeal.ui.AccessLostDialog
import com.mg.bubumeal.ui.label

/** 식단표 설정. 역할과 동반자 유무에 따라 보이는 버튼이 다르다 (docs/screen-flow.md 4장). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSettingsScreen(
    state: PlanSettingsUiState,
    onIssueCode: () -> Unit,
    onRemoveCompanion: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    onConfirmDialog: () -> Unit,
    onDismissDialog: () -> Unit,
    onBack: () -> Unit,
    /** 디버그용 접근 상실 흉내. null이면 버튼을 숨긴다. 서버가 나오면 지운다. */
    onDebugLoseAccess: (() -> Unit)?,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("식단표 설정") },
                navigationIcon = { TextButton(onClick = onBack) { Text("◀") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            val plan = state.plan ?: return@Column
            Text(plan.role.label(), style = MaterialTheme.typography.titleMedium)
            Text(
                if (plan.hasCompanion) "함께 쓰는 사람이 있어요." else "아직 혼자 쓰고 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val enabled = !state.isWorking
            val wide = Modifier.fillMaxWidth()
            if (state.showIssueCode) {
                Button(onClick = onIssueCode, enabled = enabled, modifier = wide) { Text("공유 코드 발급") }
            }
            if (state.showRemoveCompanion) {
                OutlinedButton(onClick = onRemoveCompanion, enabled = enabled, modifier = wide) { Text("동반자 연결 해제") }
            }
            if (state.showLeave) {
                OutlinedButton(onClick = onLeave, enabled = enabled, modifier = wide) { Text("식단표에서 나가기") }
            }
            if (state.showDelete) {
                Button(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = wide,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("식단표 삭제") }
            }
            if (onDebugLoseAccess != null) {
                TextButton(onClick = onDebugLoseAccess, enabled = enabled) {
                    Text("[디버그] 상대가 이 식단표를 지운 것처럼 하기", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    when (val dialog = state.dialog) {
        SettingsDialog.ACCESS_LOST -> AccessLostDialog(onConfirm = onConfirmDialog)
        null -> Unit
        else -> ConfirmDialog(dialog, enabled = !state.isWorking, onConfirm = onConfirmDialog, onDismiss = onDismissDialog)
    }
}

@Composable
private fun ConfirmDialog(dialog: SettingsDialog, enabled: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val (title, message, confirm) = when (dialog) {
        SettingsDialog.CONFIRM_REMOVE_COMPANION -> Triple(
            "동반자 해제",
            "동반자는 이 식단표를 더 이상 볼 수 없게 돼요. 식단표와 메뉴는 그대로 남고, 새 공유 코드로 다시 초대할 수 있어요.",
            "해제",
        )
        SettingsDialog.CONFIRM_LEAVE -> Triple(
            "식단표에서 나가기",
            "나가면 이 식단표를 더 이상 볼 수 없어요. 다시 참여하려면 새 공유 코드가 필요해요.",
            "나가기",
        )
        // 기획 4.2: 즉시 하드 삭제, 복구 불가, 동반자도 접근을 잃음
        SettingsDialog.CONFIRM_DELETE -> Triple(
            "식단표를 삭제할까요?",
            "식단표와 모든 끼니 기록, 발급한 공유 코드가 바로 영구 삭제되고 복구할 수 없어요. 동반자도 더 이상 볼 수 없게 돼요.",
            "영구 삭제",
        )
        SettingsDialog.NOT_ALLOWED -> Triple(
            "할 수 없는 작업",
            "공유 코드 발급, 동반자 해제, 식단표 삭제는 식단표를 만든 사람만 할 수 있어요.",
            "확인",
        )
        SettingsDialog.ACCESS_LOST -> error("AccessLostDialog로 그린다")
    }
    val isNotice = dialog == SettingsDialog.NOT_ALLOWED
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) {
                val color = if (dialog == SettingsDialog.CONFIRM_DELETE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(confirm, color = color)
            }
        },
        dismissButton = if (isNotice) null else {
            { TextButton(onClick = onDismiss, enabled = enabled) { Text("취소") } }
        },
    )
}
