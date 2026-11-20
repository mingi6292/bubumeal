package com.mg.bubumeal.ui.weekly

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.MealSlot
import com.mg.bubumeal.ui.AccessLostDialog
import com.mg.bubumeal.ui.dayLabel
import com.mg.bubumeal.ui.label
import com.mg.bubumeal.ui.weekRangeLabel
import kotlinx.datetime.LocalDate

/** 주간 식단표. 상태를 받아 그리기만 하고, 누르는 일은 모두 콜백으로 올려 보낸다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScreen(
    state: WeeklyUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
    onCellClick: (MealCell) -> Unit,
    onSwitchPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    onSave: (MealCell, String) -> Unit,
    onDelete: (MealCell) -> Unit,
    onDismissEditor: () -> Unit,
    onConfirmAccessLost: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.plan?.role?.label() ?: "") },
                actions = {
                    TextButton(onClick = onSwitchPlan) { Text("전환") }
                    TextButton(onClick = onOpenSettings) { Text("설정") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.hasNoPlan) {
                Text("식단표가 없어요.", modifier = Modifier.padding(top = 24.dp))
                return@Column
            }
            WeekNavigation(state, onPreviousWeek, onNextWeek, onCurrentWeek)
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.showEmptyHint) {
                Text(
                    "이 주에는 아직 식단이 없어요. 칸을 눌러 메뉴를 적어 보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MealTable(state, onCellClick)
        }
    }

    state.editingCell?.let { cell ->
        MealEditDialog(
            cell = cell,
            initialText = state.meals.menuOf(cell),
            onSave = { onSave(cell, it) },
            onDelete = { onDelete(cell) },
            onDismiss = onDismissEditor,
        )
    }

    if (state.accessLost) {
        AccessLostDialog(onConfirm = onConfirmAccessLost)
    }
}

@Composable
private fun WeekNavigation(
    state: WeeklyUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPreviousWeek) { Text("◀") }
            Text(weekRangeLabel(state.week), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onNextWeek) { Text("▶") }
        }
        if (!state.isCurrentWeek) {
            TextButton(onClick = onCurrentWeek) { Text("이번 주로") }
        }
    }
}

@Composable
private fun MealTable(state: WeeklyUiState, onCellClick: (MealCell) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Spacer(Modifier.weight(DAY_WEIGHT))
            MealSlot.entries.forEach { slot ->
                Text(
                    slot.label(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        state.week.days.forEach { date ->
            DayRow(date, isToday = date == state.today, state, onCellClick)
        }
    }
}

@Composable
private fun DayRow(date: LocalDate, isToday: Boolean, state: WeeklyUiState, onCellClick: (MealCell) -> Unit) {
    val rowModifier = if (isToday) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
    } else {
        Modifier
    }
    Row(
        modifier = rowModifier.fillMaxWidth().padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(dayLabel(date), modifier = Modifier.weight(DAY_WEIGHT), style = MaterialTheme.typography.labelMedium)
        MealSlot.entries.forEach { slot ->
            val cell = MealCell(date, slot)
            MealCellBox(state.meals.menuOf(cell), onClick = { onCellClick(cell) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MealCellBox(menu: String?, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (menu == null) {
            Text("+", color = MaterialTheme.colorScheme.outline)
        } else {
            Text(
                menu,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val DAY_WEIGHT = 0.9f
