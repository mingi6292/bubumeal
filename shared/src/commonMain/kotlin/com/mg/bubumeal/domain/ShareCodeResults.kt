package com.mg.bubumeal.domain

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// 공유 코드를 서버에 보냈을 때의 결과. 서버 에러 형식은 아직 없어서(기획 6장)
// 기획 5장의 에러 상황을 타입으로 먼저 정해 두고, 서버가 나오면 응답을 이 타입으로 바꿔 끼운다.

/** 기획 4.3: 발급 후 3시간 동안 유효하다. */
val SHARE_CODE_VALIDITY = 3.hours

/** 기획 4.3: 10분 안에 5번 틀리면 15분 동안 입력이 막힌다. */
const val SHARE_CODE_MAX_ATTEMPTS = 5
val SHARE_CODE_ATTEMPT_WINDOW = 10.minutes
val SHARE_CODE_BLOCK_DURATION = 15.minutes

/** 공유 코드 입력(연결) 결과. */
sealed interface JoinResult {
    /** 동반자로 연결됐다. [plan]은 새로 접근할 수 있게 된 식단표다. */
    data class Joined(val plan: PlanSummary) : JoinResult

    /** 실패. 화면이 이유마다 다른 안내를 보여 준다. */
    sealed interface Failure : JoinResult

    data object Expired : Failure
    data object AlreadyUsed : Failure

    /** 그 식단표에 이미 동반자가 있다. */
    data object PlanFull : Failure

    /** 내가 발급한 코드를 내가 입력했다. */
    data object OwnCode : Failure

    /** 없는 코드. [remainingAttempts]번 더 틀리면 막힌다. */
    data class WrongCode(val remainingAttempts: Int) : Failure

    /** 시도 제한에 걸려 [until]까지 입력할 수 없다. */
    data class Blocked(val until: Instant) : Failure
}

/** 발급된 공유 코드. */
data class IssuedCode(val code: String, val expiresAt: Instant)

/** 공유 코드 발급 결과. */
sealed interface IssueResult {
    data class Issued(val code: IssuedCode) : IssueResult

    /** 동반자가 발급하려 했다 (기획 5장 권한 에러). */
    data object NotOwner : IssueResult

    /** 식단표가 삭제되었거나 더 볼 수 없다 (기획 5장 접근 상실). */
    data object AccessLost : IssueResult
}
