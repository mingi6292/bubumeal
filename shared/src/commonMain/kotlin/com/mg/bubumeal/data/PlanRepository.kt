package com.mg.bubumeal.data

import com.mg.bubumeal.domain.CreatePlanResult
import com.mg.bubumeal.domain.PlanActionResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanSummary

// 서버가 나오면 식단표 API(BUB-13)를 부르는 구현으로 바꾼다.

interface PlanRepository {
    /** 소유한 식단표와 동반자로 참여한 식단표. 순서는 보장하지 않으므로 쓰는 쪽에서 정렬한다. */
    suspend fun accessiblePlans(): List<PlanSummary>

    /** 내 식단표를 만든다. 이미 소유한 식단표가 있으면 실패한다. */
    suspend fun create(): CreatePlanResult

    /** 소유자가 동반자를 해제한다. 식단표와 데이터는 그대로 남는다. */
    suspend fun removeCompanion(planId: PlanId): PlanActionResult

    /** 동반자가 식단표에서 나간다. */
    suspend fun leave(planId: PlanId): PlanActionResult

    /** 소유자가 식단표를 영구 삭제한다. 복구할 수 없다. */
    suspend fun delete(planId: PlanId): PlanActionResult
}
