package com.mg.bubumeal.data

import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanExitTest {
    private val mine = PlanSummary(PlanId("mine"), PlanRole.OWNER)
    private val joinedA = PlanSummary(PlanId("a"), PlanRole.COMPANION)
    private val joinedB = PlanSummary(PlanId("b"), PlanRole.COMPANION)

    @Test
    fun remainingPlans_goesToWeeklyWithOwnedPlanFirst() = runTest {
        val current = CurrentPlan().apply { select(joinedA) }
        val destination = current.moveToRemainingPlan(FakePlanRepository(listOf(joinedB, mine)))
        assertEquals(PlanExitDestination.WEEKLY, destination)
        assertEquals(mine, current.selected.value)
    }

    @Test
    fun onlyJoinedPlansRemain_selectsFirstOfThem() = runTest {
        val current = CurrentPlan().apply { select(mine) }
        val destination = current.moveToRemainingPlan(FakePlanRepository(listOf(joinedB)))
        assertEquals(PlanExitDestination.WEEKLY, destination)
        assertEquals(joinedB, current.selected.value)
    }

    @Test
    fun noPlanLeft_goesToStartAndClearsSelection() = runTest {
        val current = CurrentPlan().apply { select(mine) }
        val destination = current.moveToRemainingPlan(FakePlanRepository(emptyList()))
        assertEquals(PlanExitDestination.START, destination)
        assertNull(current.selected.value)
    }
}
