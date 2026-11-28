package com.mg.bubumeal.ui.weekly

import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.domain.Week
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class WeeklyScrollTest {

    private val week = Week.containing(LocalDate(2026, 11, 26))

    @Test
    fun todayIndex_thursday() =
        assertEquals(3, todayCardIndex(week, LocalDate(2026, 11, 26)))

    @Test
    fun todayIndex_otherWeekIsTop() =
        assertEquals(0, todayCardIndex(Week.containing(LocalDate(2026, 12, 3)), LocalDate(2026, 11, 26)))

    @Test
    fun todayIndex_yearBoundary() =
        assertEquals(4, todayCardIndex(Week.containing(LocalDate(2027, 1, 1)), LocalDate(2027, 1, 1)))

    @Test
    fun scrollKey_nullWithoutPlan() = assertNull(weeklyScrollKey(null, week))

    @Test
    fun scrollKey_changesWithPlanOrWeek() {
        val a = weeklyScrollKey(PlanSummary(PlanId("a"), PlanRole.OWNER), week)
        assertNotEquals(a, weeklyScrollKey(PlanSummary(PlanId("b"), PlanRole.COMPANION), week))
        assertNotEquals(a, weeklyScrollKey(PlanSummary(PlanId("a"), PlanRole.OWNER), week.next()))
    }

    @Test
    fun scrollKey_ignoresCompanionChange() = assertEquals(
        weeklyScrollKey(PlanSummary(PlanId("a"), PlanRole.OWNER, hasCompanion = false), week),
        weeklyScrollKey(PlanSummary(PlanId("a"), PlanRole.OWNER, hasCompanion = true), week),
    )
}
