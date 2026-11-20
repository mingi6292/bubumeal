package com.mg.bubumeal.ui.issuecode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.PlanExitDestination
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.data.moveToRemainingPlan
import com.mg.bubumeal.data.ShareCodeRepository
import com.mg.bubumeal.domain.IssueResult
import com.mg.bubumeal.domain.IssuedCode
import com.mg.bubumeal.domain.PlanId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IssueCodeUiState(
    val issued: IssuedCode? = null,
    val isIssuing: Boolean = false,
    /** 동반자가 들어왔다. 설정 화면에서 버튼을 숨기지만, 서버도 거절하므로 대비해 둔다. */
    val notOwner: Boolean = false,
    /** 식단표가 삭제되었거나 더 볼 수 없다. 안내 대화상자를 띄운다. */
    val accessLost: Boolean = false,
    /** 한 번만 처리하는 이동. 화면이 이동한 뒤 [IssueCodeViewModel.exitHandled]로 지운다. */
    val exitTo: PlanExitDestination? = null,
)

/**
 * 화면에 들어오자마자 발급하지 않고 버튼을 누를 때 발급한다.
 * 새로 발급하면 이전 코드가 무효가 되므로, 화면에 들어왔다는 이유만으로 상대에게 준 코드를 깨지 않기 위해서다.
 */
class IssueCodeViewModel(
    private val shareCodeRepository: ShareCodeRepository,
    private val currentPlan: CurrentPlan,
    private val planRepository: PlanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IssueCodeUiState())
    val uiState: StateFlow<IssueCodeUiState> = _uiState.asStateFlow()

    /** 발급을 누른 식단표. 접근 상실 뒤 갈 곳을 고를 때 후보에서 뺀다. */
    private var issuingPlanId: PlanId? = null

    fun issue() {
        val plan = currentPlan.selected.value ?: return
        val state = _uiState.value
        if (state.isIssuing || state.accessLost || state.exitTo != null) return
        issuingPlanId = plan.id
        _uiState.update { it.copy(isIssuing = true) }
        viewModelScope.launch {
            when (val result = shareCodeRepository.issue(plan.id)) {
                is IssueResult.Issued -> _uiState.update { it.copy(issued = result.code, isIssuing = false) }
                IssueResult.NotOwner -> _uiState.update { it.copy(notOwner = true, isIssuing = false) }
                IssueResult.AccessLost -> _uiState.update { it.copy(accessLost = true, isIssuing = false) }
            }
        }
    }

    /** 접근 상실 안내의 확인. 남은 식단표의 주간 식단표나 시작 화면으로 가라는 이벤트를 남긴다. */
    fun confirmAccessLost() {
        if (!_uiState.value.accessLost) return
        // 먼저 안내를 내려 두 번 눌러도 한 번만 처리한다.
        _uiState.update { it.copy(accessLost = false) }
        viewModelScope.launch {
            val destination = currentPlan.moveToRemainingPlan(planRepository, exclude = issuingPlanId)
            _uiState.update { it.copy(exitTo = destination) }
        }
    }

    fun exitHandled() = _uiState.update { it.copy(exitTo = null) }
}
