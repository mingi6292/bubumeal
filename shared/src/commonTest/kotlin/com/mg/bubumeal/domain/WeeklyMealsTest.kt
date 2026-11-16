package com.mg.bubumeal.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeeklyMealsTest {

    private val week = Week(LocalDate(2026, 11, 16))

    @Test
    fun empty_has21EmptyCells() {
        val meals = WeeklyMeals(week)
        assertEquals(21, meals.cells.size)
        assertTrue(meals.cells.all { meals.menuOf(it) == null })
        assertTrue(meals.isEmpty())
    }

    @Test
    fun cells_orderedByDayThenSlot() {
        val cells = WeeklyMeals(week).cells
        assertEquals(MealCell(LocalDate(2026, 11, 16), MealSlot.BREAKFAST), cells.first())
        assertEquals(MealCell(LocalDate(2026, 11, 16), MealSlot.LUNCH), cells[1])
        assertEquals(MealCell(LocalDate(2026, 11, 22), MealSlot.DINNER), cells.last())
    }

    @Test
    fun withMenu_setsAndKeepsOriginal() {
        val cell = MealCell(LocalDate(2026, 11, 18), MealSlot.LUNCH)
        val original = WeeklyMeals(week)
        val updated = original.withMenu(cell, "비빔밥")
        assertEquals("비빔밥", updated.menuOf(cell))
        assertNull(original.menuOf(cell))
        assertFalse(updated.isEmpty())
    }

    @Test
    fun withMenu_nullRemoves() {
        val cell = MealCell(LocalDate(2026, 11, 18), MealSlot.LUNCH)
        assertNull(WeeklyMeals(week).withMenu(cell, "비빔밥").withMenu(cell, null).menuOf(cell))
    }

    @Test
    fun withMenu_outsideWeek_throws() {
        assertFailsWith<IllegalArgumentException> {
            WeeklyMeals(week).withMenu(MealCell(LocalDate(2026, 11, 23), MealSlot.BREAKFAST), "x")
        }
    }
}
