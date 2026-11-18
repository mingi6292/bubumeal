package com.mg.bubumeal.ui.issuecode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.ShareCodeRepository
import com.mg.bubumeal.domain.IssueResult
import com.mg.bubumeal.domain.IssuedCode
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
)

/**
 * 화면에 들어오자마자 발급하지 않고 버튼을 누를 때 발급한다.
 * 새로 발급하면 이전 코드가 무효가 되므로, 화면에 들어왔다는 이유만으로 상대에게 준 코드를 깨지 않기 위해서다.
 */
class IssueCodeViewModel(
    private val shareCodeRepository: ShareCodeRepository,
    private val currentPlan: CurrentPlan,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IssueCodeUiState())
    val uiState: StateFlow<IssueCodeUiState> = _uiState.asStateFlow()

    fun issue() {
        val plan = currentPlan.selected.value ?: return
        if (_uiState.value.isIssuing) return
        _uiState.update { it.copy(isIssuing = true) }
        viewModelScope.launch {
            when (val result = shareCodeRepository.issue(plan.id)) {
                is IssueResult.Issued -> _uiState.update { it.copy(issued = result.code, isIssuing = false) }
                IssueResult.NotOwner -> _uiState.update { it.copy(notOwner = true, isIssuing = false) }
            }
        }
    }
}
