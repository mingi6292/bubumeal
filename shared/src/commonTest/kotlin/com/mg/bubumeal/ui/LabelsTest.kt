package com.mg.bubumeal.ui

import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.MealSlot
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.Week
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class LabelsTest {
    @Test
    fun planRoleLabels() {
        assertEquals("내 식단표", PlanRole.OWNER.label())
        assertEquals("참여 중인 식단표", PlanRole.COMPANION.label())
    }

    @Test
    fun mealSlotLabels() =
        assertEquals(listOf("아침", "점심", "저녁"), MealSlot.entries.map { it.label() })

    @Test
    fun dayLabel_hasMonthDayAndWeekday() {
        assertEquals("11/16 (월)", dayLabel(LocalDate(2026, 11, 16)))
        assertEquals("11/22 (일)", dayLabel(LocalDate(2026, 11, 22)))
    }

    @Test
    fun weekRange_sameYear() =
        assertEquals("11월 16일 ~ 11월 22일", weekRangeLabel(Week(LocalDate(2026, 11, 16))))

    @Test
    fun weekRange_acrossYears_showsYears() =
        assertEquals("2026년 12월 28일 ~ 2027년 1월 3일", weekRangeLabel(Week(LocalDate(2026, 12, 28))))

    @Test
    fun mealCellTitle() =
        assertEquals("11/16 (월) 저녁", mealCellTitle(MealCell(LocalDate(2026, 11, 16), MealSlot.DINNER)))

    @Test
    fun dateTimeLabel_inGivenTimeZone_zeroPadded() {
        val seoul = TimeZone.of("Asia/Seoul")
        assertEquals("11/18 (수) 15:05", dateTimeLabel(kotlin.time.Instant.parse("2026-11-18T06:05:00Z"), seoul))
        assertEquals("11/19 (목) 09:00", dateTimeLabel(kotlin.time.Instant.parse("2026-11-19T00:00:00Z"), seoul))
    }
}
