package com.mg.bubumeal.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.MealRepository
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.MealTextValidation
import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.domain.Week
import com.mg.bubumeal.domain.WeeklyMeals
import com.mg.bubumeal.domain.validateMealText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class WeeklyUiState(
    val plan: PlanSummary?,
    val meals: WeeklyMeals,
    val today: LocalDate,
    val isLoading: Boolean,
    val hasNoPlan: Boolean = false,
    val editingCell: MealCell? = null,
) {
    val week: Week get() = meals.week
    val isCurrentWeek: Boolean get() = today in week

    /** 로딩이 끝났고 식단표가 있는데 이 주에 메뉴가 하나도 없을 때. */
    val showEmptyHint: Boolean get() = !isLoading && plan != null && meals.isEmpty()
}

class WeeklyViewModel(
    private val mealRepository: MealRepository,
    planRepository: PlanRepository,
    currentPlan: CurrentPlan,
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    // "오늘"은 ViewModel을 만들 때 한 번만 계산한다. 자정을 넘겨 켜 둔 경우는 이번 단계에서 다루지 않는다.
    private val today = clock.todayIn(timeZone)
    private val week = MutableStateFlow(Week.containing(today))

    private val _uiState = MutableStateFlow(
        WeeklyUiState(plan = null, meals = WeeklyMeals(week.value), today = today, isLoading = true),
    )
    val uiState: StateFlow<WeeklyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (currentPlan.selected.value != null) return@launch
            val first = planRepository.accessiblePlans().sortedBy { it.role }.firstOrNull()
            if (first == null) {
                _uiState.update { it.copy(hasNoPlan = true, isLoading = false) }
            } else if (currentPlan.selected.value == null) {
                currentPlan.select(first)
            }
        }
        viewModelScope.launch {
            // 식단표나 주가 바뀔 때마다 다시 불러온다. collectLatest가 끝나지 않은 이전 로딩을 취소하므로
            // 늦게 도착한 이전 주 데이터가 화면을 덮어쓰지 않는다.
            combine(currentPlan.selected.filterNotNull(), week, ::Pair).collectLatest { (plan, week) ->
                _uiState.update {
                    it.copy(plan = plan, meals = WeeklyMeals(week), isLoading = true, hasNoPlan = false)
                }
                val meals = mealRepository.weeklyMeals(plan.id, week)
                _uiState.update { it.copy(meals = meals, isLoading = false) }
            }
        }
    }

    fun previousWeek() = week.update { it.previous() }

    fun nextWeek() = week.update { it.next() }

    fun goToCurrentWeek() {
        week.value = Week.containing(today)
    }

    fun openEditor(cell: MealCell) = _uiState.update { it.copy(editingCell = cell) }

    fun closeEditor() = _uiState.update { it.copy(editingCell = null) }

    /** 검증 결과: Valid → 저장 후 편집 창 닫기, Empty → 삭제 후 닫기, TooLong → 아무것도 하지 않음(창 유지). */
    fun saveMeal(cell: MealCell, input: String) {
        when (val result = validateMealText(input)) {
            is MealTextValidation.Valid -> persist(cell, result.text)
            MealTextValidation.Empty -> persist(cell, null)
            is MealTextValidation.TooLong -> Unit
        }
    }

    fun deleteMeal(cell: MealCell) = persist(cell, null)

    private fun persist(cell: MealCell, text: String?) {
        val plan = _uiState.value.plan ?: return
        viewModelScope.launch {
            mealRepository.saveMeal(plan.id, cell, text)
            _uiState.update {
                val closed = if (it.editingCell == cell) it.copy(editingCell = null) else it
                // 저장하는 사이 다른 식단표나 다른 주로 갔다면 지금 화면은 건드리지 않는다.
                if (closed.plan == plan && cell.date in closed.week) {
                    closed.copy(meals = closed.meals.withMenu(cell, text))
                } else {
                    closed
                }
            }
        }
    }
}
