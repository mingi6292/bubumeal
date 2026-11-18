package com.mg.bubumeal.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.ui.label
import com.mg.bubumeal.ui.message

/** 제공자 브랜드 색과 로고는 SDK를 붙일 때 각 제공자의 버튼 가이드를 따라 넣는다. */
@Composable
fun LoginScreen(state: LoginUiState, onLogin: (LoginProvider) -> Unit) {
    Column(
        modifier = Modifier.safeContentPadding().fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("부부밀", style = MaterialTheme.typography.headlineLarge)
        LoginProvider.entries.forEach { provider ->
            Button(
                onClick = { onLogin(provider) },
                enabled = state.inProgress == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${provider.label()}로 로그인")
            }
        }
        if (state.inProgress != null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.error?.let { error ->
            Text(error.message(), color = MaterialTheme.colorScheme.error)
        }
    }
}
