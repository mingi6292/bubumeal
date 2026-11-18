package com.mg.bubumeal.data

import com.mg.bubumeal.domain.IssueResult
import com.mg.bubumeal.domain.IssuedCode
import com.mg.bubumeal.domain.JoinResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.domain.SHARE_CODE_ALPHABET
import com.mg.bubumeal.domain.SHARE_CODE_ATTEMPT_WINDOW
import com.mg.bubumeal.domain.SHARE_CODE_BLOCK_DURATION
import com.mg.bubumeal.domain.SHARE_CODE_LENGTH
import com.mg.bubumeal.domain.SHARE_CODE_MAX_ATTEMPTS
import com.mg.bubumeal.domain.SHARE_CODE_VALIDITY
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 서버가 나오기 전까지 쓰는 가짜. 정해 둔 코드로 서버 에러를 하나씩 흉내 낸다.
 *
 * - [CODE_SUCCESS]: 연결 성공. 한 번 쓰면 그 뒤로는 "이미 사용됨"
 * - [CODE_EXPIRED], [CODE_USED], [CODE_FULL]: 각각 만료, 이미 사용됨, 이미 동반자 있음
 * - 내가 발급한 코드: 본인 코드 (3시간이 지나면 만료)
 * - 그 밖의 코드: 없는 코드. 이것만 시도 횟수에 센다(임시 결정, 서버 규칙 확인 필요)
 */
class FakeShareCodeRepository(
    private val planRepository: FakePlanRepository,
    private val clock: Clock = Clock.System,
    private val random: Random = Random.Default,
) : ShareCodeRepository {

    companion object {
        const val CODE_SUCCESS = "HAPPY234"
        const val CODE_EXPIRED = "LATE2345"
        const val CODE_USED = "USED2345"
        const val CODE_FULL = "FULL2345"
    }

    private val issuedByPlan = mutableMapOf<PlanId, IssuedCode>()
    private val failures = mutableListOf<Instant>()
    private var blockedUntil: Instant? = null
    private var successUsed = false
    private var joinedCount = 0

    override suspend fun join(code: String): JoinResult {
        val now = clock.now()
        blockedUntil?.let { if (now < it) return JoinResult.Blocked(it) else blockedUntil = null }

        issuedByPlan.values.firstOrNull { it.code == code }?.let { own ->
            return if (now < own.expiresAt) JoinResult.OwnCode else JoinResult.Expired
        }
        return when (code) {
            CODE_SUCCESS -> if (successUsed) JoinResult.AlreadyUsed else joinNewPlan()
            CODE_EXPIRED -> JoinResult.Expired
            CODE_USED -> JoinResult.AlreadyUsed
            CODE_FULL -> JoinResult.PlanFull
            else -> recordFailure(now)
        }
    }

    override suspend fun issue(planId: PlanId): IssueResult {
        val plan = planRepository.accessiblePlans().firstOrNull { it.id == planId }
        if (plan?.role != PlanRole.OWNER) return IssueResult.NotOwner
        // 새로 발급하면 이전 코드를 덮어써서 바로 무효가 된다.
        val issued = IssuedCode(newCode(), clock.now() + SHARE_CODE_VALIDITY)
        issuedByPlan[planId] = issued
        return IssueResult.Issued(issued)
    }

    private fun joinNewPlan(): JoinResult {
        successUsed = true
        joinedCount++
        val plan = PlanSummary(PlanId("joined-by-code-$joinedCount"), PlanRole.COMPANION, hasCompanion = true)
        planRepository.add(plan)
        return JoinResult.Joined(plan)
    }

    private fun recordFailure(now: Instant): JoinResult {
        failures.removeAll { now - it >= SHARE_CODE_ATTEMPT_WINDOW }
        failures += now
        if (failures.size >= SHARE_CODE_MAX_ATTEMPTS) {
            failures.clear()
            val until = now + SHARE_CODE_BLOCK_DURATION
            blockedUntil = until
            return JoinResult.Blocked(until)
        }
        return JoinResult.WrongCode(remainingAttempts = SHARE_CODE_MAX_ATTEMPTS - failures.size)
    }

    private fun newCode(): String {
        val taken = issuedByPlan.values.map { it.code }.toSet()
        while (true) {
            val code = String(CharArray(SHARE_CODE_LENGTH) { SHARE_CODE_ALPHABET[random.nextInt(SHARE_CODE_ALPHABET.length)] })
            if (code !in taken) return code
        }
    }
}
