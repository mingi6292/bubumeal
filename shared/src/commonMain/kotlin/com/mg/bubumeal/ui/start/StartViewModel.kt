package com.mg.bubumeal.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.domain.CreatePlanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StartUiState(
    val isCreating: Boolean = false,
    /** 이미 내 식단표가 있어 만들지 못했다. */
    val alreadyOwned: Boolean = false,
    /** 한 번만 처리하는 이동. 화면이 이동한 뒤 [StartViewModel.navigationHandled]로 지운다. */
    val goToWeekly: Boolean = false,
)

class StartViewModel(
    private val planRepository: PlanRepository,
    private val currentPlan: CurrentPlan,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    fun create() {
        val state = _uiState.value
        // 만드는 중이거나 이미 만들어 이동하는 중이면 무시한다. 두 번 눌러 두 번 만들지 않게 한다.
        if (state.isCreating || state.goToWeekly) return
        _uiState.update { it.copy(isCreating = true, alreadyOwned = false) }
        viewModelScope.launch {
            when (val result = planRepository.create()) {
                is CreatePlanResult.Created -> {
                    currentPlan.select(result.plan)
                    _uiState.update { it.copy(isCreating = false, goToWeekly = true) }
                }
                CreatePlanResult.AlreadyOwned -> _uiState.update { it.copy(isCreating = false, alreadyOwned = true) }
            }
        }
    }

    fun navigationHandled() = _uiState.update { it.copy(goToWeekly = false) }
}
