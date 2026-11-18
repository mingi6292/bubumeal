package com.mg.bubumeal.domain

import kotlin.jvm.JvmInline

/** 식단표 식별자. 서버가 정하는 값이라 형식을 가정하지 않는다. */
@JvmInline
value class PlanId(val value: String)

/** 이 사용자가 그 식단표에서 맡은 역할. 선언 순서가 목록에 보이는 순서다(소유 먼저). */
enum class PlanRole { OWNER, COMPANION }

/** 접근할 수 있는 식단표 하나. 식단표 이름은 아직 기획에 없다(임시 결정). */
data class PlanSummary(val id: PlanId, val role: PlanRole)
