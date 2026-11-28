package com.mg.bubumeal.ui.weekly

import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.domain.Week
import kotlinx.datetime.LocalDate

// 주간 식단표 자동 스크롤 판단. 이번 주를 "새로 열었을 때"만 오늘 카드로 가고,
// 사용자가 보던 자리는 빼앗지 않는다(설계 문서 4장).

/** 처음 보여 줄 카드 위치. 이 주에 오늘이 있으면 그 카드, 없으면 맨 위(월요일). */
fun todayCardIndex(week: Week, today: LocalDate): Int =
    week.days.indexOf(today).coerceAtLeast(0)

/**
 * 스크롤 기준. 이 값이 바뀔 때만 스크롤한다. 식단표는 id로만 본다(동반자 연결만 바뀐 같은 식단표는 그대로).
 * 화면 회전 뒤에도 남도록 `rememberSaveable`에 넣기 쉬운 String이다. 식단표가 아직 없으면 null.
 */
fun weeklyScrollKey(plan: PlanSummary?, week: Week): String? =
    plan?.let { "${it.id.value}|${week.start}" }
