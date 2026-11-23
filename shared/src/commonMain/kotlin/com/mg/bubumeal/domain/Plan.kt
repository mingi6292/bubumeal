package com.mg.bubumeal.domain

import kotlin.jvm.JvmInline

/** 식단표 식별자. 서버가 정하는 값이라 형식을 가정하지 않는다. */
@JvmInline
value class PlanId(val value: String)

/** 이 사용자가 그 식단표에서 맡은 역할. 선언 순서가 목록에 보이는 순서다(소유 먼저). */
enum class PlanRole { OWNER, COMPANION }

/**
 * 접근할 수 있는 식단표 하나. 식단표 이름은 아직 기획에 없다(임시 결정).
 * [hasCompanion]: 동반자가 연결되어 있는지. 동반자 본인에게는 항상 true다.
 */
data class PlanSummary(val id: PlanId, val role: PlanRole, val hasCompanion: Boolean = false)

/** 식단표 만들기 결과 (기획 4.2: 한 사람이 소유하는 식단표는 하나). */
sealed interface CreatePlanResult {
    data class Created(val plan: PlanSummary) : CreatePlanResult
    data object AlreadyOwned : CreatePlanResult
}

/** 동반자 해제, 나가기, 삭제 결과. */
sealed interface PlanActionResult {
    data object Success : PlanActionResult

    /** 역할이 맞지 않음. 예: 동반자가 삭제/해제를 부름, 소유자가 나가기를 부름 (기획 5장 권한). */
    data object NotAllowed : PlanActionResult

    /** 연결이 해제되었거나 식단표가 삭제되어 더 이상 볼 수 없음 (기획 5장 접근 상실). */
    data object AccessLost : PlanActionResult
}

/**
 * 식단표 안의 데이터를 읽고 쓴 결과. 접근을 잃었으면 [Lost].
 * 서버 응답 형태는 아직 정해지지 않았다(기획 9장, 임시 결정).
 */
sealed interface PlanAccess<out T> {
    data class Granted<T>(val value: T) : PlanAccess<T>
    data object Lost : PlanAccess<Nothing>
}
