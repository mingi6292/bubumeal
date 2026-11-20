package com.mg.bubumeal.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 접근 상실 안내 (기획 5장). 주간 식단표와 설정 화면이 함께 쓴다.
 * 바깥을 누르거나 뒤로 가기로 닫히지 않는다. 볼 식단표가 없으니 확인을 눌러 다른 곳으로 가야 한다.
 */
@Composable
fun AccessLostDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(ACCESS_LOST_TITLE) },
        text = { Text(ACCESS_LOST_MESSAGE) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("확인") } },
    )
}
