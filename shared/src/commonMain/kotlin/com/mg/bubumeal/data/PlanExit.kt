package com.mg.bubumeal.data

/** 보던 식단표를 더 볼 수 없게 된 뒤(삭제, 나가기, 접근 상실) 갈 곳 (docs/screen-flow.md 5장). */
enum class PlanExitDestination { WEEKLY, START }

/**
 * 남은 식단표가 있으면 그중 하나(소유 먼저)를 고르고 [PlanExitDestination.WEEKLY],
 * 없으면 선택을 비우고 [PlanExitDestination.START].
 */
suspend fun CurrentPlan.moveToRemainingPlan(planRepository: PlanRepository): PlanExitDestination {
    val next = planRepository.accessiblePlans().sortedBy { it.role }.firstOrNull()
    return if (next == null) {
        clear()
        PlanExitDestination.START
    } else {
        select(next)
        PlanExitDestination.WEEKLY
    }
}
