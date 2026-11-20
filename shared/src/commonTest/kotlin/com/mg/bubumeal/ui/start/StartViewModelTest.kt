package com.mg.bubumeal.ui.start

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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun create_addsOwnedPlanAndSelectsIt() = runTest {
        val plans = FakePlanRepository(emptyList())
        val current = CurrentPlan()
        val vm = StartViewModel(plans, current)
        vm.create()
        advanceUntilIdle()

        val created = plans.accessiblePlans().single()
        assertEquals(PlanRole.OWNER, created.role)
        assertEquals(created, current.selected.value)
        assertTrue(vm.uiState.value.goToWeekly)
        assertFalse(vm.uiState.value.isCreating)

        vm.navigationHandled()
        assertFalse(vm.uiState.value.goToWeekly)
    }

    @Test
    fun create_whenAlreadyOwning_showsErrorAndStays() = runTest {
        val mine = PlanSummary(PlanId("mine"), PlanRole.OWNER)
        val current = CurrentPlan()
        val vm = StartViewModel(FakePlanRepository(listOf(mine)), current)
        vm.create()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.alreadyOwned)
        assertFalse(vm.uiState.value.goToWeekly)
        assertNull(current.selected.value)
    }

    @Test
    fun create_twiceQuickly_createsOnce() = runTest {
        val plans = FakePlanRepository(emptyList())
        val vm = StartViewModel(plans, CurrentPlan())
        vm.create()
        vm.create()
        advanceUntilIdle()
        assertEquals(1, plans.accessiblePlans().size)
        assertFalse(vm.uiState.value.alreadyOwned)
    }
}
