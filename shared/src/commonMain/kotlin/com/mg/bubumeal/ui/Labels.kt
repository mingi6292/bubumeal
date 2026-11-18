package com.mg.bubumeal.ui

import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.MealSlot
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.Week
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

// 화면에 보이는 문구. 날짜 형식은 플랫폼 포매터 대신 직접 만들어 Android와 iOS가 똑같이 보이게 한다.

/** 식단표 이름이 아직 없어서(임시 결정) 역할로 부른다. */
fun PlanRole.label(): String = when (this) {
    PlanRole.OWNER -> "내 식단표"
    PlanRole.COMPANION -> "참여 중인 식단표"
}

fun MealSlot.label(): String = when (this) {
    MealSlot.BREAKFAST -> "아침"
    MealSlot.LUNCH -> "점심"
    MealSlot.DINNER -> "저녁"
}

private const val WEEKDAYS = "월화수목금토일"

/** 예: `11/16 (월)` */
fun dayLabel(date: LocalDate): String =
    "${date.month.number}/${date.day} (${WEEKDAYS[date.dayOfWeek.isoDayNumber - 1]})"

/** 예: `11월 16일 ~ 11월 22일`. 해가 바뀌는 주에만 연도를 붙인다. */
fun weekRangeLabel(week: Week): String {
    val withYear = week.start.year != week.end.year
    fun format(date: LocalDate): String {
        val monthDay = "${date.month.number}월 ${date.day}일"
        return if (withYear) "${date.year}년 $monthDay" else monthDay
    }
    return "${format(week.start)} ~ ${format(week.end)}"
}

/** 끼니 편집 창 제목. 예: `11/16 (월) 저녁` */
fun mealCellTitle(cell: MealCell): String = "${dayLabel(cell.date)} ${cell.slot.label()}"
