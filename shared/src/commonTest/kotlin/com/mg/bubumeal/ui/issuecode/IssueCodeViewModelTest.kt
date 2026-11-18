package com.mg.bubumeal.ui.issuecode

import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.FakePlanRepository
import com.mg.bubumeal.data.FakeShareCodeRepository
import com.mg.bubumeal.data.MutableClock
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class IssueCodeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val mine = PlanSummary(PlanId("mine"), PlanRole.OWNER)
    private val joined = PlanSummary(PlanId("joined"), PlanRole.COMPANION)
    private val clock = MutableClock()
    private val repo = FakeShareCodeRepository(FakePlanRepository(listOf(mine, joined)), clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(plan: PlanSummary) = IssueCodeViewModel(repo, CurrentPlan().apply { select(plan) })

    @Test
    fun initially_nothingIssued() {
        val state = viewModel(mine).uiState.value
        assertNull(state.issued)
        assertFalse(state.notOwner)
    }

    @Test
    fun issue_owner_showsCodeValidForThreeHours() = runTest {
        val vm = viewModel(mine)
        vm.issue()
        advanceUntilIdle()
        val issued = assertNotNull(vm.uiState.value.issued)
        assertEquals(clock.now + 3.hours, issued.expiresAt)
        assertFalse(vm.uiState.value.isIssuing)
    }

    @Test
    fun issue_again_replacesCode() = runTest {
        val vm = viewModel(mine)
        vm.issue()
        advanceUntilIdle()
        val first = vm.uiState.value.issued
        vm.issue()
        advanceUntilIdle()
        assertNotEquals(first, vm.uiState.value.issued)
    }

    @Test
    fun issue_companion_notOwner() = runTest {
        val vm = viewModel(joined)
        vm.issue()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.notOwner)
        assertNull(vm.uiState.value.issued)
    }
}
