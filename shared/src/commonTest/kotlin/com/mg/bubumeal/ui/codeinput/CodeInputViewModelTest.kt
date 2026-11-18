package com.mg.bubumeal.ui.codeinput

import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.FakePlanRepository
import com.mg.bubumeal.data.FakeShareCodeRepository
import com.mg.bubumeal.data.MutableClock
import com.mg.bubumeal.domain.IssueResult
import com.mg.bubumeal.domain.JoinResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.data.ShareCodeRepository
import com.mg.bubumeal.domain.ShareCodeValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CodeInputViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val mine = PlanSummary(PlanId("mine"), PlanRole.OWNER)
    private val currentPlan = CurrentPlan().apply { select(mine) }
    private val repo = FakeShareCodeRepository(FakePlanRepository(listOf(mine)), MutableClock())

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun submit_badFormat_showsFormatErrorWithoutCallingServer() = runTest {
        var called = false
        val spy = object : ShareCodeRepository by repo {
            override suspend fun join(code: String): JoinResult {
                called = true
                return repo.join(code)
            }
        }
        val vm = CodeInputViewModel(spy, currentPlan)
        vm.submit("abc")
        advanceUntilIdle()
        assertEquals(CodeInputError.Format(ShareCodeValidation.WrongLength(3)), vm.uiState.value.error)
        assertFalse(called)
    }

    @Test
    fun submit_success_selectsJoinedPlanAndFinishes() = runTest {
        val vm = CodeInputViewModel(repo, currentPlan)
        vm.submit("  ${FakeShareCodeRepository.CODE_SUCCESS.lowercase()} ")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.joined)
        assertEquals(PlanRole.COMPANION, currentPlan.selected.value?.role)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun submit_serverRejects_showsRejection() = runTest {
        val vm = CodeInputViewModel(repo, currentPlan)
        vm.submit(FakeShareCodeRepository.CODE_EXPIRED)
        advanceUntilIdle()
        assertEquals(CodeInputError.Rejected(JoinResult.Expired), vm.uiState.value.error)
        assertFalse(vm.uiState.value.joined)
        assertEquals(mine, currentPlan.selected.value)
    }

    @Test
    fun submit_ownCode_showsOwnCode() = runTest {
        val code = (repo.issue(mine.id) as IssueResult.Issued).code.code
        val vm = CodeInputViewModel(repo, currentPlan)
        vm.submit(code)
        advanceUntilIdle()
        assertEquals(CodeInputError.Rejected(JoinResult.OwnCode), vm.uiState.value.error)
    }

    @Test
    fun submit_whileSubmitting_ignoresSecondTap() = runTest {
        var calls = 0
        val slow = object : ShareCodeRepository by repo {
            override suspend fun join(code: String): JoinResult {
                calls++
                delay(1_000)
                return repo.join(code)
            }
        }
        val vm = CodeInputViewModel(slow, currentPlan)
        vm.submit("WRNG2345")
        advanceTimeBy(10)
        assertTrue(vm.uiState.value.isSubmitting)
        vm.submit("WRNG2345")
        advanceUntilIdle()
        assertEquals(1, calls)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun clearError_removesError() = runTest {
        val vm = CodeInputViewModel(repo, currentPlan)
        vm.submit("")
        advanceUntilIdle()
        vm.clearError()
        assertNull(vm.uiState.value.error)
    }
}
