package com.mg.bubumeal.data

import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.PlanAccess
import com.mg.bubumeal.domain.MealSlot
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.Week
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeMealRepositoryTest {
    private val planA = PlanId("a")
    private val planB = PlanId("b")
    private val week = Week(LocalDate(2026, 11, 16))
    private val monLunch = MealCell(LocalDate(2026, 11, 16), MealSlot.LUNCH)

    @Test
    fun weekWithoutData_isEmpty() = runTest {
        val meals = FakeMealRepository().weeklyMeals(planA, week).granted()
        assertEquals(week, meals.week)
        assertTrue(meals.isEmpty())
    }

    @Test
    fun saveThenLoad_returnsMenu() = runTest {
        val repo = FakeMealRepository()
        repo.saveMeal(planA, monLunch, "김치찌개")
        assertEquals("김치찌개", repo.weeklyMeals(planA, week).granted().menuOf(monLunch))
    }

    @Test
    fun saveNull_deletesMenu() = runTest {
        val repo = FakeMealRepository(mapOf(planA to mapOf(monLunch to "김치찌개")))
        repo.saveMeal(planA, monLunch, null)
        assertTrue(repo.weeklyMeals(planA, week).granted().isEmpty())
    }

    @Test
    fun plansAndWeeks_areIsolated() = runTest {
        val repo = FakeMealRepository()
        repo.saveMeal(planA, monLunch, "김치찌개")
        assertTrue(repo.weeklyMeals(planB, week).granted().isEmpty())
        assertTrue(repo.weeklyMeals(planA, week.next()).granted().isEmpty())
        assertTrue(repo.weeklyMeals(planA, week.previous()).granted().isEmpty())
    }

    @Test
    fun lostPlan_returnsLostForLoadAndSave() = runTest {
        val accessible = mutableSetOf(planA, planB)
        val repo = FakeMealRepository(isAccessible = { it in accessible })
        accessible -= planA
        assertEquals(PlanAccess.Lost, repo.weeklyMeals(planA, week))
        assertEquals(PlanAccess.Lost, repo.saveMeal(planA, monLunch, "김치찌개"))
        assertEquals(PlanAccess.Granted(Unit), repo.saveMeal(planB, monLunch, "김치찌개"))
    }
}
