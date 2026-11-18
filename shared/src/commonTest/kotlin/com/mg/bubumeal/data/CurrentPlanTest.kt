package com.mg.bubumeal.data

import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurrentPlanTest {
    @Test
    fun startsEmpty_thenHoldsSelection() {
        val current = CurrentPlan()
        assertNull(current.selected.value)
        val plan = PlanSummary(PlanId("a"), PlanRole.OWNER)
        current.select(plan)
        assertEquals(plan, current.selected.value)
    }
}
