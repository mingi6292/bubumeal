package com.mg.bubumeal.data

import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.Week
import com.mg.bubumeal.domain.WeeklyMeals

// 서버가 나오기 전까지 쓰는 가짜. 메모리에만 두므로 앱 프로세스가 끝나면 사라진다.

class FakeMealRepository(initial: Map<PlanId, Map<MealCell, String>> = emptyMap()) : MealRepository {

    private val menusByPlan: MutableMap<PlanId, MutableMap<MealCell, String>> =
        initial.mapValuesTo(mutableMapOf()) { (_, menus) -> menus.toMutableMap() }

    override suspend fun weeklyMeals(planId: PlanId, week: Week): WeeklyMeals {
        val menus = menusByPlan[planId].orEmpty().filterKeys { it.date in week }
        return WeeklyMeals(week, menus)
    }

    override suspend fun saveMeal(planId: PlanId, cell: MealCell, text: String?) {
        val menus = menusByPlan.getOrPut(planId) { mutableMapOf() }
        if (text == null) menus.remove(cell) else menus[cell] = text
    }
}
