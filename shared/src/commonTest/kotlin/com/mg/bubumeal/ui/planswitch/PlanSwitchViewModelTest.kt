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
import kotlin.test.assertTrue

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

    // screen-flow 4장: 소유한 식단표가 없는 사용자에게는 "내 식단표 만들기"가 보인다.

    @Test
    fun createOwn_visibleOnlyWithoutOwnedPlan() = runTest {
        val withOwned = viewModel()
        val joinedOnly = PlanSwitchViewModel(FakePlanRepository(listOf(joined)), CurrentPlan())
        advanceUntilIdle()
        assertFalse(withOwned.uiState.value.showCreateOwn)
        assertTrue(joinedOnly.uiState.value.showCreateOwn)
    }

    @Test
    fun createOwn_selectsNewPlanAndAsksToGoBack() = runTest {
        val repo = FakePlanRepository(listOf(joined))
        val current = CurrentPlan().apply { select(joined) }
        val vm = PlanSwitchViewModel(repo, current)
        advanceUntilIdle()
        vm.createOwn()
        vm.createOwn() // 두 번 눌러도 하나만 만든다
        advanceUntilIdle()
        val created = repo.accessiblePlans().single { it.role == PlanRole.OWNER }
        assertEquals(created, current.selected.value)
        assertTrue(vm.uiState.value.created)
        assertFalse(vm.uiState.value.showCreateOwn)
    }
}
