package com.mg.bubumeal.data

import com.mg.bubumeal.domain.PlanSummary

// 서버가 나오기 전까지 쓰는 가짜. 메모리의 목록을 돌려주고, 공유 코드로 연결하면 목록에 더한다.

class FakePlanRepository(initial: List<PlanSummary>) : PlanRepository {
    private val plans = initial.toMutableList()

    override suspend fun accessiblePlans(): List<PlanSummary> = plans.toList()

    /** 가짜 공유 코드 저장소가 연결에 성공했을 때 부른다. */
    fun add(plan: PlanSummary) {
        plans += plan
    }
}
