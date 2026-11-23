package com.mg.bubumeal.data

import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.PlanAccess
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.Week
import com.mg.bubumeal.domain.WeeklyMeals

// 서버가 나오기 전까지 쓰는 가짜. 메모리에만 두므로 앱 프로세스가 끝나면 사라진다.
// [isAccessible]로 가짜 식단표 저장소에 물어, 없어진 식단표면 접근 상실을 돌려준다.

class FakeMealRepository(
    initial: Map<PlanId, Map<MealCell, String>> = emptyMap(),
    private val isAccessible: (PlanId) -> Boolean = { true },
) : MealRepository {

    private val menusByPlan: MutableMap<PlanId, MutableMap<MealCell, String>> =
        initial.mapValuesTo(mutableMapOf()) { (_, menus) -> menus.toMutableMap() }

    override suspend fun weeklyMeals(planId: PlanId, week: Week): PlanAccess<WeeklyMeals> {
        if (!isAccessible(planId)) return PlanAccess.Lost
        val menus = menusByPlan[planId].orEmpty().filterKeys { it.date in week }
        return PlanAccess.Granted(WeeklyMeals(week, menus))
    }

    override suspend fun saveMeal(planId: PlanId, cell: MealCell, text: String?): PlanAccess<Unit> {
        if (!isAccessible(planId)) return PlanAccess.Lost
        val menus = menusByPlan.getOrPut(planId) { mutableMapOf() }
        if (text == null) menus.remove(cell) else menus[cell] = text
        return PlanAccess.Granted(Unit)
    }
}
