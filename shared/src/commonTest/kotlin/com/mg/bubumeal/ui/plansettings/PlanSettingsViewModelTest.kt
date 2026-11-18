package com.mg.bubumeal.ui.plansettings

import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.FakePlanRepository
import com.mg.bubumeal.data.PlanExitDestination
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.domain.PlanActionResult
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlanSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val mineAlone = PlanSummary(PlanId("mine"), PlanRole.OWNER, hasCompanion = false)
    private val mineShared = mineAlone.copy(hasCompanion = true)
    private val joined = PlanSummary(PlanId("joined"), PlanRole.COMPANION, hasCompanion = true)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private class Fixture(plans: List<PlanSummary>, selected: PlanSummary) {
        val repo = FakePlanRepository(plans)
        val current = CurrentPlan().apply { select(selected) }
        val vm = PlanSettingsViewModel(repo, current)
    }

    private fun TestScope.fixture(plans: List<PlanSummary>, selected: PlanSummary = plans.first()) =
        Fixture(plans, selected).also { advanceUntilIdle() }

    // screen-flow 4장: 역할과 동반자 유무에 따라 보이는 버튼

    @Test
    fun ownerWithoutCompanion_seesIssueAndDelete() = runTest {
        val state = fixture(listOf(mineAlone)).vm.uiState.value
        assertTrue(state.showIssueCode)
        assertFalse(state.showRemoveCompanion)
        assertTrue(state.showDelete)
        assertFalse(state.showLeave)
    }

    @Test
    fun ownerWithCompanion_seesRemoveCompanionAndDelete() = runTest {
        val state = fixture(listOf(mineShared)).vm.uiState.value
        assertFalse(state.showIssueCode)
        assertTrue(state.showRemoveCompanion)
        assertTrue(state.showDelete)
        assertFalse(state.showLeave)
    }

    @Test
    fun companion_seesOnlyLeave() = runTest {
        val state = fixture(listOf(joined)).vm.uiState.value
        assertFalse(state.showIssueCode)
        assertFalse(state.showRemoveCompanion)
        assertFalse(state.showDelete)
        assertTrue(state.showLeave)
    }

    @Test
    fun usesLatestPlanFromRepository_notStaleSelection() = runTest {
        // 전에 골라 둔 값에는 동반자가 없다고 되어 있어도, 저장소의 최신 값을 따른다.
        val f = fixture(listOf(mineShared), selected = mineAlone)
        assertTrue(f.vm.uiState.value.showRemoveCompanion)
        assertEquals(mineShared, f.current.selected.value)
    }

    // 동반자 해제

    @Test
    fun removeCompanion_asksFirstThenStaysWithIssueButton() = runTest {
        val f = fixture(listOf(mineShared))
        f.vm.requestRemoveCompanion()
        assertEquals(SettingsDialog.CONFIRM_REMOVE_COMPANION, f.vm.uiState.value.dialog)
        assertTrue(f.repo.accessiblePlans().single().hasCompanion) // 확인 전에는 아무 일도 없다

        f.vm.confirmDialog()
        advanceUntilIdle()
        val state = f.vm.uiState.value
        assertNull(state.dialog)
        assertNull(state.exitTo)
        assertTrue(state.showIssueCode)
        assertFalse(state.showRemoveCompanion)
        assertEquals(mineAlone, f.current.selected.value)
    }

    @Test
    fun dismissDialog_doesNothing() = runTest {
        val f = fixture(listOf(mineAlone))
        f.vm.requestDelete()
        f.vm.dismissDialog()
        advanceUntilIdle()
        assertNull(f.vm.uiState.value.dialog)
        assertEquals(listOf(mineAlone), f.repo.accessiblePlans())
    }

    // 삭제, 나가기 뒤 이동 (screen-flow 5장)

    @Test
    fun delete_withJoinedPlanLeft_exitsToWeeklyOfThatPlan() = runTest {
        val f = fixture(listOf(mineAlone, joined))
        f.vm.requestDelete()
        assertEquals(SettingsDialog.CONFIRM_DELETE, f.vm.uiState.value.dialog)
        f.vm.confirmDialog()
        advanceUntilIdle()
        assertEquals(PlanExitDestination.WEEKLY, f.vm.uiState.value.exitTo)
        assertEquals(joined, f.current.selected.value)
        assertEquals(listOf(joined), f.repo.accessiblePlans())
    }

    @Test
    fun delete_withNothingLeft_exitsToStart() = runTest {
        val f = fixture(listOf(mineAlone))
        f.vm.requestDelete()
        f.vm.confirmDialog()
        advanceUntilIdle()
        assertEquals(PlanExitDestination.START, f.vm.uiState.value.exitTo)
        assertNull(f.current.selected.value)

        f.vm.exitHandled()
        assertNull(f.vm.uiState.value.exitTo)
    }

    @Test
    fun leave_withOwnedPlanLeft_exitsToWeeklyOfOwnedPlan() = runTest {
        val f = fixture(listOf(mineAlone, joined), selected = joined)
        f.vm.requestLeave()
        assertEquals(SettingsDialog.CONFIRM_LEAVE, f.vm.uiState.value.dialog)
        f.vm.confirmDialog()
        advanceUntilIdle()
        assertEquals(PlanExitDestination.WEEKLY, f.vm.uiState.value.exitTo)
        assertEquals(mineAlone, f.current.selected.value)
    }

    @Test
    fun leave_withNothingLeft_exitsToStart() = runTest {
        val f = fixture(listOf(joined))
        f.vm.requestLeave()
        f.vm.confirmDialog()
        advanceUntilIdle()
        assertEquals(PlanExitDestination.START, f.vm.uiState.value.exitTo)
    }

    // 권한 없음, 접근 상실

    @Test
    fun companionCallingDelete_getsNotAllowed() = runTest {
        // 버튼은 숨기지만, 서버가 거절하는 경우에 대비한다.
        val f = fixture(listOf(joined))
        f.vm.requestDelete()
        f.vm.confirmDialog()
        advanceUntilIdle()
        assertEquals(SettingsDialog.NOT_ALLOWED, f.vm.uiState.value.dialog)
        assertNull(f.vm.uiState.value.exitTo)
        assertEquals(listOf(joined), f.repo.accessiblePlans())

        f.vm.confirmDialog() // 안내를 닫는다
        assertNull(f.vm.uiState.value.dialog)
    }

    @Test
    fun companionCallingRemoveCompanion_getsNotAllowed() = runTest {
        val f = fixture(listOf(joined))
        f.vm.requestRemoveCompanion()
        f.vm.confirmDialog()
        advanceUntilIdle()
        assertEquals(SettingsDialog.NOT_ALLOWED, f.vm.uiState.value.dialog)
    }

    @Test
    fun accessLostDuringAction_showsNoticeThenExits() = runTest {
        val f = fixture(listOf(mineAlone, joined), selected = joined)
        f.repo.loseAccess(joined.id)
        f.vm.requestLeave()
        f.vm.confirmDialog()
        advanceUntilIdle()
        assertEquals(SettingsDialog.ACCESS_LOST, f.vm.uiState.value.dialog)
        assertNull(f.vm.uiState.value.exitTo) // 안내를 확인하기 전에는 이동하지 않는다

        f.vm.confirmDialog()
        advanceUntilIdle()
        assertNull(f.vm.uiState.value.dialog)
        assertEquals(PlanExitDestination.WEEKLY, f.vm.uiState.value.exitTo)
        assertEquals(mineAlone, f.current.selected.value)
    }

    @Test
    fun planAlreadyGoneWhenOpened_showsAccessLost() = runTest {
        val f = fixture(listOf(mineAlone), selected = joined)
        assertEquals(SettingsDialog.ACCESS_LOST, f.vm.uiState.value.dialog)
        assertFalse(f.vm.uiState.value.isLoading)
    }

    @Test
    fun whileWorking_dialogCannotBeDismissed() = runTest {
        // 서버가 느릴 때 삭제 중에 대화상자를 닫으면, 뒤로 가기로 화면을 떠나 이동이 사라질 수 있다.
        val repo = FakePlanRepository(listOf(mineAlone))
        val slow = object : PlanRepository by repo {
            override suspend fun delete(planId: PlanId): PlanActionResult {
                delay(1_000)
                return repo.delete(planId)
            }
        }
        val vm = PlanSettingsViewModel(slow, CurrentPlan().apply { select(mineAlone) })
        advanceUntilIdle()
        vm.requestDelete()
        vm.confirmDialog()
        advanceTimeBy(10)
        assertTrue(vm.uiState.value.isWorking)
        vm.dismissDialog()
        assertEquals(SettingsDialog.CONFIRM_DELETE, vm.uiState.value.dialog)

        advanceUntilIdle()
        assertEquals(PlanExitDestination.START, vm.uiState.value.exitTo)
    }
}
