package com.mg.bubumeal.data

import com.mg.bubumeal.domain.IssueResult
import com.mg.bubumeal.domain.JoinResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.domain.ShareCodeValidation
import com.mg.bubumeal.domain.validateShareCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** 테스트에서 시간을 직접 돌리는 시계. */
class MutableClock(var now: Instant = Instant.parse("2026-11-18T03:00:00Z")) : Clock {
    override fun now(): Instant = now
    fun advance(by: Duration) {
        now += by
    }
}

class FakeShareCodeRepositoryTest {
    private val mine = PlanSummary(PlanId("mine"), PlanRole.OWNER)
    private val joined = PlanSummary(PlanId("joined"), PlanRole.COMPANION)
    private val clock = MutableClock()
    private val plans = FakePlanRepository(listOf(mine, joined))
    private val repo = FakeShareCodeRepository(plans, clock)

    private suspend fun failWrongCode(times: Int) = repeat(times) { repo.join("WRNG2345") }

    @Test
    fun join_successCode_addsCompanionPlan() = runTest {
        val result = assertIs<JoinResult.Joined>(repo.join(FakeShareCodeRepository.CODE_SUCCESS))
        assertEquals(PlanRole.COMPANION, result.plan.role)
        assertTrue(result.plan in plans.accessiblePlans())
    }

    @Test
    fun join_sameSuccessCodeTwice_alreadyUsed() = runTest {
        repo.join(FakeShareCodeRepository.CODE_SUCCESS)
        assertEquals(JoinResult.AlreadyUsed, repo.join(FakeShareCodeRepository.CODE_SUCCESS))
    }

    @Test
    fun join_specialCodes_returnMatchingErrors() = runTest {
        assertEquals(JoinResult.Expired, repo.join(FakeShareCodeRepository.CODE_EXPIRED))
        assertEquals(JoinResult.AlreadyUsed, repo.join(FakeShareCodeRepository.CODE_USED))
        assertEquals(JoinResult.PlanFull, repo.join(FakeShareCodeRepository.CODE_FULL))
    }

    @Test
    fun join_ownIssuedCode_ownCode() = runTest {
        val issued = assertIs<IssueResult.Issued>(repo.issue(mine.id)).code
        assertEquals(JoinResult.OwnCode, repo.join(issued.code))
    }

    @Test
    fun join_unknownCode_countsDownRemainingAttempts() = runTest {
        assertEquals(JoinResult.WrongCode(remainingAttempts = 4), repo.join("WRNG2345"))
        assertEquals(JoinResult.WrongCode(remainingAttempts = 3), repo.join("WRNG2345"))
    }

    @Test
    fun join_fifthWrongWithinTenMinutes_blocksForFifteenMinutes() = runTest {
        failWrongCode(4)
        assertEquals(JoinResult.Blocked(until = clock.now + 15.minutes), repo.join("WRNG2345"))
    }

    @Test
    fun join_whileBlocked_evenSuccessCodeIsBlocked() = runTest {
        failWrongCode(5)
        val until = clock.now + 15.minutes
        clock.advance(14.minutes)
        assertEquals(JoinResult.Blocked(until), repo.join(FakeShareCodeRepository.CODE_SUCCESS))
    }

    @Test
    fun join_afterBlockEnds_allowedWithFullAttempts() = runTest {
        failWrongCode(5)
        clock.advance(15.minutes)
        assertIs<JoinResult.Joined>(repo.join(FakeShareCodeRepository.CODE_SUCCESS))
        assertEquals(JoinResult.WrongCode(remainingAttempts = 4), repo.join("WRNG2345"))
    }

    @Test
    fun join_failuresOlderThanTenMinutes_doNotCount() = runTest {
        failWrongCode(4)
        clock.advance(10.minutes)
        assertEquals(JoinResult.WrongCode(remainingAttempts = 4), repo.join("WRNG2345"))
    }

    @Test
    fun issue_ownerGetsValidCodeForThreeHours() = runTest {
        val issued = assertIs<IssueResult.Issued>(repo.issue(mine.id)).code
        assertIs<ShareCodeValidation.Valid>(validateShareCode(issued.code))
        assertEquals(clock.now + 3.hours, issued.expiresAt)
    }

    @Test
    fun issue_companionIsNotOwner() = runTest {
        assertEquals(IssueResult.NotOwner, repo.issue(joined.id))
    }

    @Test
    fun issue_again_oldCodeNoLongerOwnCode() = runTest {
        val first = assertIs<IssueResult.Issued>(repo.issue(mine.id)).code
        val second = assertIs<IssueResult.Issued>(repo.issue(mine.id)).code
        assertNotEquals(first.code, second.code)
        assertIs<JoinResult.WrongCode>(repo.join(first.code))
        assertEquals(JoinResult.OwnCode, repo.join(second.code))
    }

    @Test
    fun join_ownCodeAfterThreeHours_expired() = runTest {
        val issued = assertIs<IssueResult.Issued>(repo.issue(mine.id)).code
        clock.advance(3.hours)
        assertEquals(JoinResult.Expired, repo.join(issued.code))
    }

    @Test
    fun issue_lostPlan_accessLost() = runTest {
        plans.loseAccess(mine.id)
        assertEquals(IssueResult.AccessLost, repo.issue(mine.id))
    }

    @Test
    fun deletingPlan_removesItsIssuedCode() = runTest {
        // 기획 4.2: 삭제하면 발급한 공유 코드 기록도 함께 영구 삭제된다.
        val old = assertIs<IssueResult.Issued>(repo.issue(mine.id)).code.code
        plans.delete(mine.id)
        assertIs<JoinResult.WrongCode>(repo.join(old))
    }
}
