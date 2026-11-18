package com.mg.bubumeal.data

import com.mg.bubumeal.domain.PlanSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 지금 보고 있는 식단표. 주간 식단표 화면과 전환 화면이 함께 본다.
// 서버가 나와도 이 클래스는 그대로 쓰고, 필요하면 마지막 선택을 기기에 저장하는 기능을 더한다.

class CurrentPlan {
    private val _selected = MutableStateFlow<PlanSummary?>(null)

    /** 아직 고르지 않았으면 null. */
    val selected: StateFlow<PlanSummary?> = _selected.asStateFlow()

    fun select(plan: PlanSummary) {
        _selected.value = plan
    }
}
