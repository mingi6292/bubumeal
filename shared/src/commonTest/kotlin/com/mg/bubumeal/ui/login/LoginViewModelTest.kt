package com.mg.bubumeal.ui.login

import com.mg.bubumeal.data.FakePlanRepository
import com.mg.bubumeal.data.SocialLogin
import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.domain.ProviderToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 다음에 돌려줄 결과를 정해 두는 가짜. [gate]를 주면 그것이 완료될 때까지 기다린다. */
private class ScriptedSocialLogin(var next: LoginResult, var gate: CompletableDeferred<Unit>? = null) : SocialLogin {
    val calls = mutableListOf<LoginProvider>()

    override suspend fun login(provider: LoginProvider): LoginResult {
        calls += provider
        gate?.await()
        return next
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val success = LoginResult.Success(ProviderToken(LoginProvider.KAKAO, "token"))
    private val somePlan = PlanSummary(PlanId("mine"), PlanRole.OWNER)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(login: SocialLogin, plans: List<PlanSummary> = listOf(somePlan)) =
        LoginViewModel(login, FakePlanRepository(plans))

    @Test
    fun login_success_withPlans_goesToWeekly() = runTest {
        val vm = viewModel(ScriptedSocialLogin(success))
        vm.login(LoginProvider.KAKAO)
        advanceUntilIdle()
        assertEquals(LoginDestination.WEEKLY, vm.uiState.value.destination)
        assertNull(vm.uiState.value.inProgress)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun login_success_noPlans_goesToStart() = runTest {
        val vm = viewModel(ScriptedSocialLogin(success), plans = emptyList())
        vm.login(LoginProvider.KAKAO)
        advanceUntilIdle()
        assertEquals(LoginDestination.START, vm.uiState.value.destination)
    }

    @Test
    fun login_inProgress_showsProvider() = runTest {
        val vm = viewModel(ScriptedSocialLogin(success, gate = CompletableDeferred()))
        vm.login(LoginProvider.NAVER)
        advanceUntilIdle()
        assertEquals(LoginProvider.NAVER, vm.uiState.value.inProgress)
    }

    @Test
    fun login_cancelled_showsError() = runTest {
        val vm = viewModel(ScriptedSocialLogin(LoginResult.Cancelled))
        vm.login(LoginProvider.GOOGLE)
        advanceUntilIdle()
        assertEquals(LoginResult.Cancelled, vm.uiState.value.error)
        assertNull(vm.uiState.value.destination)
        assertNull(vm.uiState.value.inProgress)
    }

    @Test
    fun login_failed_showsError() = runTest {
        val vm = viewModel(ScriptedSocialLogin(LoginResult.Failed))
        vm.login(LoginProvider.APPLE)
        advanceUntilIdle()
        assertEquals(LoginResult.Failed, vm.uiState.value.error)
    }

    @Test
    fun login_whileInProgress_ignored() = runTest {
        val login = ScriptedSocialLogin(success, gate = CompletableDeferred())
        val vm = viewModel(login)
        vm.login(LoginProvider.KAKAO)
        advanceUntilIdle()
        vm.login(LoginProvider.GOOGLE)
        advanceUntilIdle()
        assertEquals(listOf(LoginProvider.KAKAO), login.calls)
    }

    @Test
    fun login_afterSuccess_ignored() = runTest {
        val login = ScriptedSocialLogin(success)
        val vm = viewModel(login)
        vm.login(LoginProvider.KAKAO)
        advanceUntilIdle()
        vm.login(LoginProvider.APPLE)
        advanceUntilIdle()
        assertEquals(1, login.calls.size)
    }

    @Test
    fun login_retryAfterCancel_clearsError() = runTest {
        val login = ScriptedSocialLogin(LoginResult.Cancelled)
        val vm = viewModel(login)
        vm.login(LoginProvider.KAKAO)
        advanceUntilIdle()

        val gate = CompletableDeferred<Unit>()
        login.next = success
        login.gate = gate
        vm.login(LoginProvider.KAKAO)
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(LoginDestination.WEEKLY, vm.uiState.value.destination)
    }
}
