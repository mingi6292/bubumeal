package com.mg.bubumeal.domain

import kotlinx.datetime.LocalDate

/** 식단표의 한 칸: 어느 날의 어느 끼니. */
data class MealCell(val date: LocalDate, val slot: MealSlot)

/**
 * 한 주의 식단. 바꿀 수 없는 값이라 [withMenu]는 새 객체를 돌려준다.
 * 메뉴가 없는 칸은 [menus]에 키가 없다.
 */
data class WeeklyMeals(val week: Week, val menus: Map<MealCell, String> = emptyMap()) {

    /** 21칸. 날짜 순서, 같은 날 안에서는 [MealSlot] 순서. */
    val cells: List<MealCell>
        get() = week.days.flatMap { date -> MealSlot.entries.map { MealCell(date, it) } }

    fun menuOf(cell: MealCell): String? = menus[cell]

    /** [text]가 null이면 그 칸을 지운다. 텍스트 검증은 호출하는 쪽에서 [validateMealText]로 먼저 한다. */
    fun withMenu(cell: MealCell, text: String?): WeeklyMeals {
        require(cell.date in week) { "이 주에 없는 날짜다: ${cell.date}" }
        return copy(menus = if (text == null) menus - cell else menus + (cell to text))
    }

    fun isEmpty(): Boolean = menus.isEmpty()
}
