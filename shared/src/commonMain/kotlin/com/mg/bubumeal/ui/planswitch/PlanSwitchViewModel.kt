package com.mg.bubumeal.ui.planswitch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.domain.PlanId
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
)

class PlanSwitchViewModel(
    planRepository: PlanRepository,
    private val currentPlan: CurrentPlan,
) : ViewModel() {

    private val plans = MutableStateFlow<List<PlanSummary>?>(null)

    val uiState: StateFlow<PlanSwitchUiState> =
        combine(plans, currentPlan.selected) { plans, selected ->
            PlanSwitchUiState(plans = plans.orEmpty(), selectedId = selected?.id, isLoading = plans == null)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PlanSwitchUiState())

    init {
        viewModelScope.launch {
            plans.value = planRepository.accessiblePlans().sortedBy { it.role }
        }
    }

    // 주간 식단표 ViewModel이 CurrentPlan을 지켜보고 있어서, 고르기만 하면 그쪽이 다시 불러온다.
    fun select(plan: PlanSummary) = currentPlan.select(plan)
}
