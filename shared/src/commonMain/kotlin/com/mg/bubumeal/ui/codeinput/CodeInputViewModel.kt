package com.mg.bubumeal.ui.codeinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.ShareCodeRepository
import com.mg.bubumeal.domain.JoinResult
import com.mg.bubumeal.domain.ShareCodeValidation
import com.mg.bubumeal.domain.validateShareCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 입력 화면에 보여 줄 에러. 앱에서 먼저 걸러낸 형식 오류와 서버가 거절한 이유 두 갈래다. */
sealed interface CodeInputError {
    data class Format(val validation: ShareCodeValidation) : CodeInputError
    data class Rejected(val failure: JoinResult.Failure) : CodeInputError
}

data class CodeInputUiState(
    val isSubmitting: Boolean = false,
    val error: CodeInputError? = null,
    /** 연결에 성공했다. 화면이 이 값을 보고 주간 식단표로 이동한다. */
    val joined: Boolean = false,
)

/** 입력 중인 글은 화면이 들고 있는다(한글 조합 중 커서가 튀지 않도록). 보낼 때만 넘겨받는다. */
class CodeInputViewModel(
    private val shareCodeRepository: ShareCodeRepository,
    private val currentPlan: CurrentPlan,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodeInputUiState())
    val uiState: StateFlow<CodeInputUiState> = _uiState.asStateFlow()

    fun submit(input: String) {
        val state = _uiState.value
        if (state.isSubmitting || state.joined) return

        val code = when (val validation = validateShareCode(input)) {
            is ShareCodeValidation.Valid -> validation.code
            else -> {
                _uiState.update { it.copy(error = CodeInputError.Format(validation)) }
                return
            }
        }
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = shareCodeRepository.join(code)) {
                is JoinResult.Joined -> {
                    currentPlan.select(result.plan)
                    _uiState.update { it.copy(isSubmitting = false, joined = true) }
                }
                is JoinResult.Failure ->
                    _uiState.update { it.copy(isSubmitting = false, error = CodeInputError.Rejected(result)) }
            }
        }
    }

    /** 입력을 고치기 시작하면 이전 에러를 지운다. */
    fun clearError() = _uiState.update { it.copy(error = null) }
}
