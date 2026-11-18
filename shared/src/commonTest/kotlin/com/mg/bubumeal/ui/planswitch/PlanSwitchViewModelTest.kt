package com.mg.bubumeal.ui.planswitch

import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.FakePlanRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class PlanSwitchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val mine = PlanSummary(PlanId("mine"), PlanRole.OWNER)
    private val joined = PlanSummary(PlanId("joined"), PlanRole.COMPANION)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(current: CurrentPlan = CurrentPlan()) =
        PlanSwitchViewModel(FakePlanRepository(listOf(joined, mine)), current)

    @Test
    fun plans_ownedFirst() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(listOf(mine, joined), vm.uiState.value.plans)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun selectedId_followsCurrentPlan() = runTest {
        val vm = viewModel(CurrentPlan().apply { select(joined) })
        advanceUntilIdle()
        assertEquals(joined.id, vm.uiState.value.selectedId)
    }

    @Test
    fun select_updatesCurrentPlan() = runTest {
        val current = CurrentPlan().apply { select(mine) }
        val vm = viewModel(current)
        advanceUntilIdle()
        vm.select(joined)
        advanceUntilIdle()
        assertEquals(joined, current.selected.value)
        assertEquals(joined.id, vm.uiState.value.selectedId)
    }
}
