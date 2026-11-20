package com.mg.bubumeal.ui.plansettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.PlanExitDestination
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.data.moveToRemainingPlan
import com.mg.bubumeal.domain.PlanActionResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 설정 화면 위에 뜨는 대화상자 (docs/screen-flow.md 3장). */
enum class SettingsDialog {
    CONFIRM_REMOVE_COMPANION,
    CONFIRM_LEAVE,
    CONFIRM_DELETE,

    /** 더 이상 볼 수 없는 식단표. 확인하면 남은 식단표나 시작 화면으로 간다. */
    ACCESS_LOST,

    /** 역할에 맞지 않는 일을 했다. 버튼을 숨기므로 서버가 거절한 경우에만 뜬다. */
    NOT_ALLOWED,
}

data class PlanSettingsUiState(
    val plan: PlanSummary? = null,
    val isLoading: Boolean = true,
    /** 해제, 나가기, 삭제를 서버에 보내는 중. 버튼을 끈다. */
    val isWorking: Boolean = false,
    /** 대화상자는 상태다. 화면을 회전해도 떠 있어야 하고, 사용자가 닫을 때까지 남는다. */
    val dialog: SettingsDialog? = null,
    /** 한 번만 처리하는 이동. 화면이 이동한 뒤 [PlanSettingsViewModel.exitHandled]로 지운다. */
    val exitTo: PlanExitDestination? = null,
    /** 디버그 버튼을 보일지. */
    val canDebugLoseAccess: Boolean = false,
) {
    private val isOwner get() = plan?.role == PlanRole.OWNER

    // docs/screen-flow.md 4장. 동반자가 있으면 발급 버튼을 숨긴다(screen-flow 6장 임시 결정).
    val showIssueCode: Boolean get() = isOwner && plan?.hasCompanion == false
    val showRemoveCompanion: Boolean get() = isOwner && plan?.hasCompanion == true
    val showDelete: Boolean get() = isOwner
    val showLeave: Boolean get() = plan?.role == PlanRole.COMPANION
}

class PlanSettingsViewModel(
    private val planRepository: PlanRepository,
    private val currentPlan: CurrentPlan,
    /** 디버그용 접근 상실 흉내(가짜 저장소). null이면 버튼을 숨긴다. 서버가 나오면 지운다. */
    private val debugLoseAccess: ((PlanId) -> Unit)? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanSettingsUiState(canDebugLoseAccess = debugLoseAccess != null))
    val uiState: StateFlow<PlanSettingsUiState> = _uiState.asStateFlow()

    /** 이 화면이 다루는 식단표. 화면을 여는 순간 골라 둔 것으로 정하고, 나간 뒤에는 다시 고르지 않는다. */
    private val planId: PlanId? = currentPlan.selected.value?.id

    init {
        if (planId == null) {
            _uiState.update { it.copy(isLoading = false, dialog = SettingsDialog.ACCESS_LOST) }
        } else {
            viewModelScope.launch { refresh(planId) }
        }
    }

    fun requestRemoveCompanion() = ask(SettingsDialog.CONFIRM_REMOVE_COMPANION)

    fun requestLeave() = ask(SettingsDialog.CONFIRM_LEAVE)

    fun requestDelete() = ask(SettingsDialog.CONFIRM_DELETE)

    /**
     * 확인 대화상자는 그냥 닫는다. 접근 상실 안내는 닫을 수 없다(볼 식단표가 없으므로 확인해야 한다).
     * 서버에 보내는 중에도 닫지 않는다. 닫으면 화면을 떠날 수 있게 되어 끝난 뒤의 이동이 사라진다.
     */
    fun dismissDialog() = _uiState.update {
        if (it.isWorking || it.dialog == SettingsDialog.ACCESS_LOST) it else it.copy(dialog = null)
    }

    /** 대화상자의 확인 버튼. */
    fun confirmDialog() {
        val state = _uiState.value
        if (state.isWorking) return
        when (state.dialog) {
            SettingsDialog.CONFIRM_REMOVE_COMPANION -> perform(planId) { planRepository.removeCompanion(it) }
            SettingsDialog.CONFIRM_LEAVE -> perform(planId, exitOnSuccess = true) { planRepository.leave(it) }
            SettingsDialog.CONFIRM_DELETE -> perform(planId, exitOnSuccess = true) { planRepository.delete(it) }
            SettingsDialog.ACCESS_LOST -> exit()
            SettingsDialog.NOT_ALLOWED -> _uiState.update { it.copy(dialog = null) }
            null -> Unit
        }
    }

    fun exitHandled() = _uiState.update { it.copy(exitTo = null) }

    /** 디버그: 이 식단표를 볼 수 없게 만든 뒤 바로 다시 읽어, 접근 상실 안내가 곧바로 뜨게 한다. */
    fun debugLoseAccess() {
        val lose = debugLoseAccess ?: return
        val id = planId ?: return
        if (_uiState.value.isWorking || _uiState.value.dialog != null) return
        lose(id)
        viewModelScope.launch { refresh(id) }
    }

    private fun ask(dialog: SettingsDialog) = _uiState.update {
        if (it.isWorking || it.dialog != null || it.plan == null) it else it.copy(dialog = dialog)
    }

    private fun perform(
        planId: PlanId?,
        exitOnSuccess: Boolean = false,
        action: suspend (PlanId) -> PlanActionResult,
    ) {
        if (planId == null) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            when (action(planId)) {
                PlanActionResult.Success -> if (exitOnSuccess) {
                    exitNow()
                } else {
                    // 동반자 해제: 이 화면에 머물고, 바뀐 식단표(동반자 없음)를 다시 읽는다.
                    _uiState.update { it.copy(dialog = null) }
                    refresh(planId)
                }
                PlanActionResult.NotAllowed -> _uiState.update { it.copy(dialog = SettingsDialog.NOT_ALLOWED) }
                PlanActionResult.AccessLost -> _uiState.update { it.copy(dialog = SettingsDialog.ACCESS_LOST) }
            }
            _uiState.update { it.copy(isWorking = false) }
        }
    }

    private fun exit() {
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            exitNow()
            _uiState.update { it.copy(isWorking = false) }
        }
    }

    private suspend fun exitNow() {
        // 삭제, 나가기, 접근 상실 모두 이 식단표는 더 볼 수 없으므로 후보에서 뺀다.
        val destination = currentPlan.moveToRemainingPlan(planRepository, exclude = planId)
        _uiState.update { it.copy(dialog = null, exitTo = destination) }
    }

    /** 저장소의 최신 값을 읽는다. 골라 둔 값(CurrentPlan)은 동반자 유무가 오래되었을 수 있다. */
    private suspend fun refresh(planId: PlanId) {
        val latest = planRepository.accessiblePlans().firstOrNull { it.id == planId }
        if (latest == null) {
            _uiState.update { it.copy(isLoading = false, dialog = SettingsDialog.ACCESS_LOST) }
        } else {
            currentPlan.select(latest)
            _uiState.update { it.copy(plan = latest, isLoading = false) }
        }
    }
}
