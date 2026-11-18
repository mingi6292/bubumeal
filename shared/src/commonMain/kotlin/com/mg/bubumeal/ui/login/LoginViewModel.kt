package com.mg.bubumeal.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.data.SocialLogin
import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 로그인 뒤 갈 곳 (docs/screen-flow.md 5장). */
enum class LoginDestination { WEEKLY, START }

data class LoginUiState(
    /** 로그인 창이 떠 있는 제공자. null이 아니면 모든 버튼을 끈다. */
    val inProgress: LoginProvider? = null,
    val error: LoginResult.Failure? = null,
    /** null이 아니면 화면이 그곳으로 이동한다. */
    val destination: LoginDestination? = null,
)

class LoginViewModel(
    private val socialLogin: SocialLogin,
    private val planRepository: PlanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(provider: LoginProvider) {
        val state = _uiState.value
        // 로그인 중이거나 이미 성공해 이동하는 중이면 무시한다.
        if (state.inProgress != null || state.destination != null) return
        _uiState.update { it.copy(inProgress = provider, error = null) }
        viewModelScope.launch {
            when (val result = socialLogin.login(provider)) {
                is LoginResult.Success -> {
                    // 서버가 나오면 여기서 제공자 토큰을 서버로 보내고 서비스 토큰을 받는다.
                    val destination = if (planRepository.accessiblePlans().isEmpty()) {
                        LoginDestination.START
                    } else {
                        LoginDestination.WEEKLY
                    }
                    _uiState.update { it.copy(inProgress = null, destination = destination) }
                }
                is LoginResult.Failure -> _uiState.update { it.copy(inProgress = null, error = result) }
            }
        }
    }
}
