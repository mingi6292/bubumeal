package com.mg.bubumeal.ui.weekly

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.MealSlot
import com.mg.bubumeal.ui.AccessLostDialog
import com.mg.bubumeal.ui.dayLabel
import com.mg.bubumeal.ui.label
import com.mg.bubumeal.ui.weekRangeLabel
import kotlinx.datetime.LocalDate

/** 주간 식단표. 하루 카드 7개를 세로로 늘어놓는다. 상태를 받아 그리기만 하고, 누르는 일은 모두 콜백으로 올려 보낸다. */
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
                title = {
                    // 제목을 누르면 식단표 전환 화면으로 간다.
                    state.plan?.let { plan ->
                        TextButton(onClick = onSwitchPlan) {
                            Text(
                                "${plan.role.label()} ▼",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                // 상단 바는 높이가 고정이라, 글자를 크게 해도 두 줄로 꺾여 잘리지 않게 한 줄로 줄인다.
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text("설정", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.hasNoPlan) {
                Text("식단표가 없어요.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp))
                return@Column
            }
            WeekHeader(state, onPreviousWeek, onNextWeek, onCurrentWeek)
            DayCardList(state, onCellClick)
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

/** 목록 위에 고정되는 부분: 주 이동, 이번 주로, 로딩 막대, 빈 주 안내. */
@Composable
private fun WeekHeader(
    state: WeeklyUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WeekArrow("‹", onPreviousWeek)
            Text(
                weekRangeLabel(state.week),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            WeekArrow("›", onNextWeek)
        }
        if (!state.isCurrentWeek) {
            OutlinedButton(
                onClick = onCurrentWeek,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text("이번 주로", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        }
        if (state.showEmptyHint) {
            Text(
                "이 주에는 아직 식단이 없어요. 칸을 눌러 메뉴를 적어 보세요.",
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekArrow(symbol: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Text(symbol, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DayCardList(state: WeeklyUiState, onCellClick: (MealCell) -> Unit) {
    val listState = rememberLazyListState()
    // 마지막으로 스크롤한 기준(식단표 id + 주). 회전하거나 다른 화면에서 돌아와도 남아 있어서,
    // 같은 식단표의 같은 주를 다시 그릴 때는 보던 자리를 그대로 둔다.
    var scrolledKey by rememberSaveable { mutableStateOf<String?>(null) }
    val key = weeklyScrollKey(state.plan, state.week)
    LaunchedEffect(key) {
        if (key != null && key != scrolledKey) {
            listState.scrollToItem(todayCardIndex(state.week, state.today))
            scrolledKey = key
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.week.days, key = { it.toString() }) { date ->
            DayCard(date, isToday = date == state.today, state, onCellClick)
        }
    }
}

@Composable
private fun DayCard(date: LocalDate, isToday: Boolean, state: WeeklyUiState, onCellClick: (MealCell) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        border = if (isToday) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = if (isToday) 4.dp else 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 5.dp),
            ) {
                Text(dayLabel(date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isToday) TodayBadge()
            }
            MealSlot.entries.forEach { slot ->
                val cell = MealCell(date, slot)
                MealRow(slot, state.meals.menuOf(cell), onClick = { onCellClick(cell) })
            }
        }
    }
}

@Composable
private fun TodayBadge() {
    Text(
        "오늘",
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimary,
    )
}

/** 끼니 한 줄. 줄 전체가 누르는 영역이고, 메뉴는 잘라내지 않고 줄바꿈한다. */
@Composable
private fun MealRow(slot: MealSlot, menu: String?, onClick: () -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            slot.label(),
            modifier = Modifier.widthIn(min = 32.dp).padding(top = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        if (menu == null) {
            Text("+ 메뉴 적기", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        } else {
            Text(menu, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
