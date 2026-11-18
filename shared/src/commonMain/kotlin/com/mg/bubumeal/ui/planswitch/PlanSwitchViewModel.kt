package com.mg.bubumeal.ui.planswitch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.domain.CreatePlanResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanSwitchUiState(
    val plans: List<PlanSummary> = emptyList(), // 소유 먼저 (PlanRole 순서)
    val selectedId: PlanId? = null,
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    /** 한 번만 처리하는 이동: 내 식단표를 만들었으니 주간 식단표로 돌아간다. */
    val created: Boolean = false,
) {
    /** screen-flow 4장: 소유한 식단표가 없는 사용자(동반자로만 참여 중)에게만 보인다. */
    val showCreateOwn: Boolean get() = !isLoading && plans.none { it.role == PlanRole.OWNER }
}

class PlanSwitchViewModel(
    private val planRepository: PlanRepository,
    private val currentPlan: CurrentPlan,
) : ViewModel() {

    private val plans = MutableStateFlow<List<PlanSummary>?>(null)
    private val creation = MutableStateFlow(Creation())

    private data class Creation(val inProgress: Boolean = false, val done: Boolean = false)

    val uiState: StateFlow<PlanSwitchUiState> =
        combine(plans, currentPlan.selected, creation) { plans, selected, creation ->
            PlanSwitchUiState(
                plans = plans.orEmpty(),
                selectedId = selected?.id,
                isLoading = plans == null,
                isCreating = creation.inProgress,
                created = creation.done,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PlanSwitchUiState())

    init {
        viewModelScope.launch { reload() }
    }

    // 주간 식단표 ViewModel이 CurrentPlan을 지켜보고 있어서, 고르기만 하면 그쪽이 다시 불러온다.
    fun select(plan: PlanSummary) = currentPlan.select(plan)

    fun createOwn() {
        if (creation.value.inProgress || creation.value.done) return
        creation.value = Creation(inProgress = true)
        viewModelScope.launch {
            when (val result = planRepository.create()) {
                is CreatePlanResult.Created -> {
                    currentPlan.select(result.plan)
                    reload()
                    creation.value = Creation(done = true)
                }
                // 다른 기기에서 이미 만들었다. 목록을 다시 읽으면 버튼이 사라지고 그 식단표가 보인다.
                CreatePlanResult.AlreadyOwned -> {
                    reload()
                    creation.value = Creation()
                }
            }
        }
    }

    fun createdHandled() {
        creation.value = Creation()
    }

    private suspend fun reload() {
        plans.value = planRepository.accessiblePlans().sortedBy { it.role }
    }
}
