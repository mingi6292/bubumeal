package com.mg.bubumeal.data

import com.mg.bubumeal.domain.PlanSummary

// 서버가 나오기 전까지 쓰는 가짜. 정해진 목록을 그대로 돌려준다.

class FakePlanRepository(private val plans: List<PlanSummary>) : PlanRepository {
    override suspend fun accessiblePlans(): List<PlanSummary> = plans
}
