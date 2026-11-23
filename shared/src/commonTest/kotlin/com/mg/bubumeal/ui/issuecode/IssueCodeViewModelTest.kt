package com.mg.bubumeal.ui.issuecode

import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.FakePlanRepository
import com.mg.bubumeal.data.FakeShareCodeRepository
import com.mg.bubumeal.data.MutableClock
import com.mg.bubumeal.data.PlanExitDestination
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
    private val plans = FakePlanRepository(listOf(mine, joined))
    private val repo = FakeShareCodeRepository(plans, clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(plan: PlanSummary, current: CurrentPlan = CurrentPlan().apply { select(plan) }) =
        IssueCodeViewModel(repo, current, plans)

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

    @Test
    fun issue_lostPlan_showsAccessLostThenExits() = runTest {
        val current = CurrentPlan().apply { select(mine) }
        val vm = viewModel(mine, current)
        plans.loseAccess(mine.id)
        vm.issue()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.accessLost)
        assertFalse(vm.uiState.value.notOwner)

        vm.confirmAccessLost()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.accessLost)
        assertEquals(PlanExitDestination.WEEKLY, vm.uiState.value.exitTo)
        assertEquals(joined, current.selected.value)
    }
}
