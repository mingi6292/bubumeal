package com.mg.bubumeal.data

import com.mg.bubumeal.domain.CreatePlanResult
import com.mg.bubumeal.domain.PlanActionResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakePlanRepositoryTest {
    private val mine = PlanSummary(PlanId("mine"), PlanRole.OWNER, hasCompanion = true)
    private val joined = PlanSummary(PlanId("joined"), PlanRole.COMPANION, hasCompanion = true)

    @Test
    fun create_withoutOwnedPlan_addsOwnedPlanWithoutCompanion() = runTest {
        val repo = FakePlanRepository(listOf(joined))
        val result = assertIs<CreatePlanResult.Created>(repo.create())
        assertEquals(PlanRole.OWNER, result.plan.role)
        assertFalse(result.plan.hasCompanion)
        assertEquals(listOf(joined, result.plan), repo.accessiblePlans())
    }

    @Test
    fun create_whenAlreadyOwning_fails() = runTest {
        val repo = FakePlanRepository(listOf(mine))
        assertEquals(CreatePlanResult.AlreadyOwned, repo.create())
        assertEquals(listOf(mine), repo.accessiblePlans())
    }

    @Test
    fun removeCompanion_byOwner_keepsPlanWithoutCompanion() = runTest {
        val repo = FakePlanRepository(listOf(mine))
        assertEquals(PlanActionResult.Success, repo.removeCompanion(mine.id))
        assertEquals(listOf(mine.copy(hasCompanion = false)), repo.accessiblePlans())
    }

    @Test
    fun removeCompanionAndDelete_byCompanion_areNotAllowed() = runTest {
        val repo = FakePlanRepository(listOf(joined))
        assertEquals(PlanActionResult.NotAllowed, repo.removeCompanion(joined.id))
        assertEquals(PlanActionResult.NotAllowed, repo.delete(joined.id))
        assertEquals(listOf(joined), repo.accessiblePlans())
    }

    @Test
    fun leave_byOwner_isNotAllowed() = runTest {
        val repo = FakePlanRepository(listOf(mine))
        assertEquals(PlanActionResult.NotAllowed, repo.leave(mine.id))
        assertEquals(listOf(mine), repo.accessiblePlans())
    }

    @Test
    fun leave_byCompanion_removesPlan() = runTest {
        val repo = FakePlanRepository(listOf(mine, joined))
        assertEquals(PlanActionResult.Success, repo.leave(joined.id))
        assertEquals(listOf(mine), repo.accessiblePlans())
    }

    @Test
    fun delete_byOwner_removesPlanAndAllowsCreatingAgain() = runTest {
        val repo = FakePlanRepository(listOf(mine))
        assertEquals(PlanActionResult.Success, repo.delete(mine.id))
        assertTrue(repo.accessiblePlans().isEmpty())
        assertIs<CreatePlanResult.Created>(repo.create())
    }

    @Test
    fun actionsOnLostPlan_returnAccessLost() = runTest {
        val repo = FakePlanRepository(listOf(mine, joined))
        repo.loseAccess(joined.id)
        repo.loseAccess(mine.id)
        assertTrue(repo.accessiblePlans().isEmpty())
        assertFalse(repo.isAccessible(mine.id))
        assertEquals(PlanActionResult.AccessLost, repo.leave(joined.id))
        assertEquals(PlanActionResult.AccessLost, repo.removeCompanion(mine.id))
        assertEquals(PlanActionResult.AccessLost, repo.delete(mine.id))
    }

    @Test
    fun createdPlans_getNewIds() = runTest {
        val repo = FakePlanRepository(emptyList())
        val first = assertIs<CreatePlanResult.Created>(repo.create()).plan
        repo.delete(first.id)
        val second = assertIs<CreatePlanResult.Created>(repo.create()).plan
        // 같은 id를 다시 쓰면 지운 식단표의 끼니 데이터가 되살아난 것처럼 보인다.
        assertTrue(first.id != second.id)
    }
}
