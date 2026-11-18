package com.mg.bubumeal.data

import com.mg.bubumeal.domain.CreatePlanResult
import com.mg.bubumeal.domain.PlanActionResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary

// 서버가 나오기 전까지 쓰는 가짜. 메모리의 목록 하나로 "지금 사용자가 볼 수 있는 식단표"를 흉내 낸다.
// 목록에서 빠진 식단표에 무엇을 하든 접근 상실이 된다.

class FakePlanRepository(initial: List<PlanSummary>) : PlanRepository {
    private val plans = initial.toMutableList()
    private var createdCount = 0

    override suspend fun accessiblePlans(): List<PlanSummary> = plans.toList()

    override suspend fun create(): CreatePlanResult {
        if (plans.any { it.role == PlanRole.OWNER }) return CreatePlanResult.AlreadyOwned
        createdCount++
        // 지운 식단표와 id가 겹치면 끼니 데이터가 되살아난 것처럼 보이므로 매번 새 id를 쓴다.
        val plan = PlanSummary(PlanId("created-$createdCount"), PlanRole.OWNER, hasCompanion = false)
        plans += plan
        return CreatePlanResult.Created(plan)
    }

    override suspend fun removeCompanion(planId: PlanId): PlanActionResult =
        act(planId, requiredRole = PlanRole.OWNER) { index ->
            plans[index] = plans[index].copy(hasCompanion = false)
        }

    override suspend fun leave(planId: PlanId): PlanActionResult =
        act(planId, requiredRole = PlanRole.COMPANION) { index -> plans.removeAt(index) }

    override suspend fun delete(planId: PlanId): PlanActionResult =
        act(planId, requiredRole = PlanRole.OWNER) { index -> plans.removeAt(index) }

    /** 가짜 끼니 저장소가 접근 상실을 흉내 낼 때 쓴다. */
    fun isAccessible(planId: PlanId): Boolean = plans.any { it.id == planId }

    /** 가짜 공유 코드 저장소가 연결에 성공했을 때 부른다. */
    fun add(plan: PlanSummary) {
        plans += plan
    }

    /**
     * 디버그용: 상대가 식단표를 지웠거나 나를 해제한 것처럼, 앱에 알리지 않고 목록에서 뺀다.
     * 그 뒤 그 식단표를 읽거나 바꾸면 접근 상실이 된다. 서버가 나오면 지운다.
     */
    fun loseAccess(planId: PlanId) {
        plans.removeAll { it.id == planId }
    }

    private inline fun act(planId: PlanId, requiredRole: PlanRole, change: (index: Int) -> Unit): PlanActionResult {
        val index = plans.indexOfFirst { it.id == planId }
        if (index < 0) return PlanActionResult.AccessLost
        if (plans[index].role != requiredRole) return PlanActionResult.NotAllowed
        change(index)
        return PlanActionResult.Success
    }
}
