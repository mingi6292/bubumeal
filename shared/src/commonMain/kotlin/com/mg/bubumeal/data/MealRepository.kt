package com.mg.bubumeal.data

import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.Week
import com.mg.bubumeal.domain.WeeklyMeals

// 서버가 나오면 주간 식단표 API(BUB-15)를 부르는 구현으로 바꾼다.

interface MealRepository {
    /** 데이터가 없는 주는 빈 [WeeklyMeals]를 돌려준다. */
    suspend fun weeklyMeals(planId: PlanId, week: Week): WeeklyMeals

    /** [text]가 null이면 그 칸을 지운다. 텍스트 검증은 호출하는 쪽에서 먼저 한다. */
    suspend fun saveMeal(planId: PlanId, cell: MealCell, text: String?)
}
