package com.mg.bubumeal.data

import com.mg.bubumeal.domain.PlanSummary

// 서버가 나오면 "접근 가능한 식단표 목록" API를 부르는 구현으로 바꾼다.

interface PlanRepository {
    /** 소유한 식단표와 동반자로 참여한 식단표. 순서는 보장하지 않으므로 쓰는 쪽에서 정렬한다. */
    suspend fun accessiblePlans(): List<PlanSummary>
}
