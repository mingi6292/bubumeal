package com.mg.bubumeal.ui.planswitch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.ui.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSwitchScreen(
    state: PlanSwitchUiState,
    onSelect: (PlanSummary) -> Unit,
    onEnterCode: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("식단표 전환") },
                navigationIcon = { TextButton(onClick = onBack) { Text("◀") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.plans.forEach { plan ->
                val selected = plan.id == state.selectedId
                ListItem(
                    headlineContent = { Text(plan.role.label()) },
                    trailingContent = {
                        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { onSelect(plan) },
                )
                HorizontalDivider()
            }
            Button(onClick = onEnterCode, modifier = Modifier.padding(16.dp)) {
                Text("공유 코드 입력")
            }
        }
    }
}
