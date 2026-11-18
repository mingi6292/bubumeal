package com.mg.bubumeal.ui.codeinput

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeInputScreen(
    state: CodeInputUiState,
    onSubmit: (String) -> Unit,
    onTextChange: () -> Unit,
    onBack: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val submit = { onSubmit(text) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("공유 코드 입력") },
                navigationIcon = { TextButton(onClick = onBack) { Text("◀") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "상대가 보내 준 8자리 코드를 입력하면 그 식단표에 함께 참여해요.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onTextChange()
                },
                label = { Text("공유 코드") },
                singleLine = true,
                isError = state.error != null,
                supportingText = state.error?.let { error -> { Text(error.message(timeZone)) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isSubmitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Button(onClick = submit, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                Text("참여하기")
            }
        }
    }
}
