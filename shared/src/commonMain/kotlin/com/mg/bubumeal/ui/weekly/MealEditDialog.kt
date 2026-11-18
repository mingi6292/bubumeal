package com.mg.bubumeal.ui.weekly

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.mg.bubumeal.domain.MAX_MEAL_TEXT_LENGTH
import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.MealTextValidation
import com.mg.bubumeal.domain.validateMealText
import com.mg.bubumeal.ui.mealCellTitle

/**
 * 끼니 하나의 메뉴를 입력/수정/삭제하는 창.
 * 입력 중인 글은 ViewModel이 아니라 이 창 안에 둔다. StateFlow로 TextField 값을 돌리면 한글 조합 중 커서가 튄다.
 * 50자를 넘어도 글자는 계속 칠 수 있고, 대신 저장 버튼을 끄고 안내를 보여 준다.
 */
@Composable
fun MealEditDialog(
    cell: MealCell,
    initialText: String?,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable(cell) { mutableStateOf(initialText ?: "") }
    val validation = validateMealText(text)
    val tooLong = validation is MealTextValidation.TooLong
    val length = text.trim().length

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(mealCellTitle(cell)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("메뉴를 적어 주세요") },
                isError = tooLong,
                supportingText = {
                    Text(
                        if (tooLong) {
                            "${MAX_MEAL_TEXT_LENGTH}자까지 적을 수 있어요 ($length/$MAX_MEAL_TEXT_LENGTH)"
                        } else {
                            "$length/$MAX_MEAL_TEXT_LENGTH"
                        },
                    )
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = !tooLong) { Text("저장") }
        },
        dismissButton = {
            Row {
                if (initialText != null) {
                    TextButton(onClick = onDelete) { Text("삭제") }
                }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}
