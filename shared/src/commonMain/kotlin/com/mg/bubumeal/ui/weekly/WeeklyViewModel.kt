package com.mg.bubumeal.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.MealRepository
import com.mg.bubumeal.data.PlanExitDestination
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.data.moveToRemainingPlan
import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.MealTextValidation
import com.mg.bubumeal.domain.PlanAccess
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
    /** 이 식단표를 더 볼 수 없다. 안내 대화상자를 띄운다. */
    val accessLost: Boolean = false,
    /** 한 번만 처리하는 이동. 화면이 이동한 뒤 [WeeklyViewModel.exitHandled]로 지운다. */
    val exitTo: PlanExitDestination? = null,
) {
    val week: Week get() = meals.week
    val isCurrentWeek: Boolean get() = today in week

    /** 로딩이 끝났고 식단표가 있는데 이 주에 메뉴가 하나도 없을 때. */
    val showEmptyHint: Boolean get() = !isLoading && plan != null && meals.isEmpty()
}

class WeeklyViewModel(
    private val mealRepository: MealRepository,
    private val planRepository: PlanRepository,
    private val currentPlan: CurrentPlan,
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
                // 열려 있던 편집 창은 닫는다. 다른 식단표/주의 칸을 가리키게 되기 때문이다.
                _uiState.update {
                    it.copy(
                        plan = plan,
                        meals = WeeklyMeals(week),
                        isLoading = true,
                        hasNoPlan = false,
                        editingCell = null,
                    )
                }
                when (val result = mealRepository.weeklyMeals(plan.id, week)) {
                    is PlanAccess.Granted -> _uiState.update { it.copy(meals = result.value, isLoading = false) }
                    PlanAccess.Lost -> _uiState.update { it.copy(isLoading = false, accessLost = true) }
                }
            }
        }
    }

    fun previousWeek() = week.update { it.previous() }

    fun nextWeek() = week.update { it.next() }

    fun goToCurrentWeek() {
        week.value = Week.containing(today)
    }

    /**
     * 로딩 중에는 열지 않는다. 로딩 중의 칸은 비어 보이므로, 그때 연 창에서 저장하면
     * 로딩이 끝난 뒤의 진짜 메뉴를 빈 값으로 지울 수 있다.
     */
    fun openEditor(cell: MealCell) =
        _uiState.update { if (it.isLoading || it.accessLost) it else it.copy(editingCell = cell) }

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
            if (mealRepository.saveMeal(plan.id, cell, text) == PlanAccess.Lost) {
                // 저장하는 사이 다른 식단표로 옮겼다면, 지금 보는 식단표의 일이 아니므로 안내하지 않는다.
                // 같은 식단표인지는 id로 본다. 동반자 유무 같은 정보만 바뀌어도 같은 식단표다.
                _uiState.update { if (it.plan?.id == plan.id) it.copy(editingCell = null, accessLost = true) else it }
                return@launch
            }
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

    private var exitingLostPlan = false

    /**
     * 접근 상실 안내에서 확인을 눌렀다. 남은 식단표가 있으면 이 화면에서 그 식단표를 다시 불러오고
     * (CurrentPlan이 바뀌면 위의 collectLatest가 불러온다), 없으면 시작 화면으로 가라는 이벤트를 남긴다.
     */
    fun confirmAccessLost() {
        // 안내는 이동 준비가 끝날 때까지 띄워 두고, 그 사이 다시 눌러도 한 번만 처리한다.
        if (!_uiState.value.accessLost || exitingLostPlan) return
        exitingLostPlan = true
        viewModelScope.launch {
            val destination = currentPlan.moveToRemainingPlan(planRepository, exclude = _uiState.value.plan?.id)
            exitingLostPlan = false
            _uiState.update {
                it.copy(
                    accessLost = false,
                    exitTo = if (destination == PlanExitDestination.START) destination else null,
                )
            }
        }
    }

    /** 화면이 [WeeklyUiState.exitTo]로 이동을 마쳤다. 다시 그려져도 두 번 이동하지 않게 지운다. */
    fun exitHandled() = _uiState.update { it.copy(exitTo = null) }
}
