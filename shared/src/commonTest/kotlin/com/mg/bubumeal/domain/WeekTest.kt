package com.mg.bubumeal.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class WeekTest {

    @Test
    fun containing_midweek_returnsMondayToSunday() {
        val week = Week.containing(LocalDate(2026, 11, 18))
        assertEquals(LocalDate(2026, 11, 16), week.start)
        assertEquals(LocalDate(2026, 11, 22), week.end)
    }

    @Test
    fun containing_sunday_isLastDayOfWeek() =
        assertEquals(LocalDate(2026, 11, 16), Week.containing(LocalDate(2026, 11, 22)).start)

    @Test
    fun containing_monday_isFirstDay() =
        assertEquals(LocalDate(2026, 11, 16), Week.containing(LocalDate(2026, 11, 16)).start)

    @Test
    fun containing_yearBoundary() {
        val week = Week.containing(LocalDate(2027, 1, 1))
        assertEquals(LocalDate(2026, 12, 28), week.start)
        assertEquals(LocalDate(2027, 1, 3), week.days.last())
    }

    @Test
    fun days_areSevenConsecutiveDays() {
        val days = Week(LocalDate(2026, 11, 16)).days
        assertEquals(7, days.size)
        assertEquals((16..22).map { LocalDate(2026, 11, it) }, days)
    }

    @Test
    fun previousAndNext() {
        val week = Week(LocalDate(2026, 11, 16))
        assertEquals(LocalDate(2026, 11, 9), week.previous().start)
        assertEquals(LocalDate(2026, 11, 23), week.next().start)
    }

    @Test
    fun contains_onlyDatesInWeek() {
        val week = Week(LocalDate(2026, 11, 16))
        assertTrue(LocalDate(2026, 11, 22) in week)
        assertFalse(LocalDate(2026, 11, 23) in week)
    }

    @Test
    fun constructor_rejectsNonMonday() {
        assertFailsWith<IllegalArgumentException> { Week(LocalDate(2026, 11, 17)) }
    }

    @Test
    fun current_dependsOnTimeZone() {
        // 2026-11-15T15:30Z = 서울 2026-11-16(월) 00:30 = UTC 2026-11-15(일) 15:30
        val clock = object : Clock {
            override fun now() = Instant.parse("2026-11-15T15:30:00Z")
        }
        assertEquals(LocalDate(2026, 11, 16), Week.current(clock, TimeZone.of("Asia/Seoul")).start)
        assertEquals(LocalDate(2026, 11, 9), Week.current(clock, TimeZone.UTC).start)
    }

    @Test
    fun mealSlot_order() =
        assertEquals(listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER), MealSlot.entries)
}
