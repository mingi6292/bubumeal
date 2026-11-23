package com.mg.bubumeal.ui.weekly

import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.FakeMealRepository
import com.mg.bubumeal.data.FakePlanRepository
import com.mg.bubumeal.data.MealRepository
import com.mg.bubumeal.data.PlanExitDestination
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.data.granted
import com.mg.bubumeal.domain.MAX_MEAL_TEXT_LENGTH
import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.MealSlot
import com.mg.bubumeal.domain.PlanAccess
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.domain.Week
import com.mg.bubumeal.domain.WeeklyMeals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    // 2026-11-18(수) 서울 낮 12시
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-18T03:00:00Z")
    }
    private val seoul = TimeZone.of("Asia/Seoul")
    private val thisWeek = Week(LocalDate(2026, 11, 16))
    private val mine = PlanSummary(PlanId("mine"), PlanRole.OWNER)
    private val joined = PlanSummary(PlanId("joined"), PlanRole.COMPANION)
    private val monLunch = MealCell(LocalDate(2026, 11, 16), MealSlot.LUNCH)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        meals: MealRepository = FakeMealRepository(),
        plans: List<PlanSummary> = listOf(joined, mine), // 일부러 참여 식단표를 먼저 둔다
        current: CurrentPlan = CurrentPlan(),
    ) = WeeklyViewModel(meals, FakePlanRepository(plans), current, clock, seoul)

    @Test
    fun init_selectsOwnedPlanAndLoadsCurrentWeek() = runTest {
        val current = CurrentPlan()
        val vm = viewModel(current = current)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(mine, state.plan)
        assertEquals(thisWeek, state.week)
        assertFalse(state.isLoading)
        assertTrue(state.isCurrentWeek)
        assertEquals(mine, current.selected.value)
    }

    @Test
    fun init_keepsAlreadySelectedPlan() = runTest {
        val current = CurrentPlan().apply { select(joined) }
        val vm = viewModel(current = current)
        advanceUntilIdle()
        assertEquals(joined, vm.uiState.value.plan)
    }

    @Test
    fun init_withNoPlans_setsHasNoPlan() = runTest {
        val vm = viewModel(plans = emptyList())
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.hasNoPlan)
        assertNull(state.plan)
        assertFalse(state.showEmptyHint)
        assertFalse(state.isLoading)
    }

    @Test
    fun emptyWeek_showsEmptyHint() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showEmptyHint)
        assertEquals(21, vm.uiState.value.meals.cells.size)
    }

    @Test
    fun nextAndPrevious_changeWeek() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.nextWeek()
        advanceUntilIdle()
        assertEquals(thisWeek.next(), vm.uiState.value.week)
        vm.previousWeek()
        vm.previousWeek()
        advanceUntilIdle()
        assertEquals(thisWeek.previous(), vm.uiState.value.week)
        assertFalse(vm.uiState.value.isCurrentWeek)
    }

    @Test
    fun goToCurrentWeek_returnsToToday() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.nextWeek()
        vm.nextWeek()
        vm.goToCurrentWeek()
        advanceUntilIdle()
        assertEquals(thisWeek, vm.uiState.value.week)
    }

    @Test
    fun saveMeal_persistsAcrossWeekNavigation() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.saveMeal(monLunch, "김치찌개")
        advanceUntilIdle()
        vm.nextWeek()
        advanceUntilIdle()
        vm.previousWeek()
        advanceUntilIdle()
        assertEquals("김치찌개", vm.uiState.value.meals.menuOf(monLunch))
        assertFalse(vm.uiState.value.showEmptyHint)
    }

    @Test
    fun saveMeal_trimsAndAccepts50Chars() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.openEditor(monLunch)
        vm.saveMeal(monLunch, "  " + "가".repeat(MAX_MEAL_TEXT_LENGTH) + " ")
        advanceUntilIdle()
        assertEquals("가".repeat(MAX_MEAL_TEXT_LENGTH), vm.uiState.value.meals.menuOf(monLunch))
        assertNull(vm.uiState.value.editingCell)
    }

    @Test
    fun saveMeal_tooLong_isRejectedAndEditorStaysOpen() = runTest {
        val repo = FakeMealRepository()
        val vm = viewModel(meals = repo)
        advanceUntilIdle()
        vm.openEditor(monLunch)
        vm.saveMeal(monLunch, "가".repeat(MAX_MEAL_TEXT_LENGTH + 1))
        advanceUntilIdle()
        assertNull(vm.uiState.value.meals.menuOf(monLunch))
        assertEquals(monLunch, vm.uiState.value.editingCell)
        assertNull(repo.weeklyMeals(mine.id, thisWeek).granted().menuOf(monLunch))
    }

    @Test
    fun saveMeal_blank_deletesMenu() = runTest {
        val repo = FakeMealRepository(mapOf(mine.id to mapOf(monLunch to "김치찌개")))
        val vm = viewModel(meals = repo)
        advanceUntilIdle()
        vm.saveMeal(monLunch, "   ")
        advanceUntilIdle()
        assertNull(vm.uiState.value.meals.menuOf(monLunch))
        assertNull(repo.weeklyMeals(mine.id, thisWeek).granted().menuOf(monLunch))
    }

    @Test
    fun deleteMeal_removesMenuAndClosesEditor() = runTest {
        val repo = FakeMealRepository(mapOf(mine.id to mapOf(monLunch to "김치찌개")))
        val vm = viewModel(meals = repo)
        advanceUntilIdle()
        vm.openEditor(monLunch)
        vm.deleteMeal(monLunch)
        advanceUntilIdle()
        assertNull(vm.uiState.value.meals.menuOf(monLunch))
        assertNull(vm.uiState.value.editingCell)
        assertNull(repo.weeklyMeals(mine.id, thisWeek).granted().menuOf(monLunch))
    }

    @Test
    fun openAndCloseEditor() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.openEditor(monLunch)
        assertEquals(monLunch, vm.uiState.value.editingCell)
        vm.closeEditor()
        assertNull(vm.uiState.value.editingCell)
    }

    @Test
    fun planChange_reloadsSameWeek() = runTest {
        val repo = FakeMealRepository(mapOf(joined.id to mapOf(monLunch to "카레")))
        val current = CurrentPlan()
        val vm = viewModel(meals = repo, current = current)
        advanceUntilIdle()
        vm.nextWeek()
        advanceUntilIdle()
        vm.previousWeek()
        advanceUntilIdle()
        current.select(joined)
        advanceUntilIdle()
        assertEquals(joined, vm.uiState.value.plan)
        assertEquals(thisWeek, vm.uiState.value.week)
        assertEquals("카레", vm.uiState.value.meals.menuOf(monLunch))
        current.select(mine)
        advanceUntilIdle()
        assertNull(vm.uiState.value.meals.menuOf(monLunch))
    }

    @Test
    fun rapidWeekChange_showsOnlyLatestWeek() = runTest {
        val vm = viewModel(meals = SlowWeekRepository(slowWeek = thisWeek.next()))
        advanceUntilIdle()
        vm.nextWeek()
        advanceTimeBy(10)
        vm.nextWeek()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(thisWeek.next().next(), state.week)
        assertEquals(state.week, state.meals.week)
        assertFalse(state.meals.menus.containsValue(LATE))
    }

    @Test
    fun openEditor_whileLoading_isIgnored() = runTest {
        // 로딩 중인 빈 칸으로 편집 창을 열면, 로딩이 끝난 뒤 진짜 메뉴를 빈 값으로 덮어 지울 수 있다.
        val vm = viewModel(meals = SlowWeekRepository(slowWeek = thisWeek.next()))
        advanceUntilIdle()
        vm.nextWeek()
        advanceTimeBy(10)
        vm.openEditor(MealCell(LocalDate(2026, 11, 23), MealSlot.LUNCH))
        assertNull(vm.uiState.value.editingCell)
    }

    @Test
    fun reload_closesOpenEditor() = runTest {
        val current = CurrentPlan()
        val vm = viewModel(current = current)
        advanceUntilIdle()
        vm.openEditor(monLunch)
        current.select(joined)
        advanceUntilIdle()
        assertNull(vm.uiState.value.editingCell)
    }

    @Test
    fun accessLostOnLoad_showsNoticeThenMovesToRemainingPlan() = runTest {
        val plans = FakePlanRepository(listOf(joined, mine))
        val current = CurrentPlan().apply { select(joined) }
        plans.loseAccess(joined.id)
        val vm = WeeklyViewModel(FakeMealRepository(isAccessible = plans::isAccessible), plans, current, clock, seoul)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.accessLost)
        assertFalse(vm.uiState.value.isLoading)

        vm.confirmAccessLost()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.accessLost)
        assertNull(state.exitTo) // 남은 식단표가 있으면 이 화면에 머물며 그 식단표를 보여 준다
        assertEquals(mine, state.plan)
        assertEquals(mine, current.selected.value)
    }

    @Test
    fun accessLostOnSave_closesEditorAndShowsNotice() = runTest {
        val plans = FakePlanRepository(listOf(mine))
        val vm = WeeklyViewModel(FakeMealRepository(isAccessible = plans::isAccessible), plans, CurrentPlan(), clock, seoul)
        advanceUntilIdle()
        vm.openEditor(monLunch)
        plans.loseAccess(mine.id)
        vm.saveMeal(monLunch, "김치찌개")
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.accessLost)
        assertNull(state.editingCell)
        assertNull(state.meals.menuOf(monLunch))
    }

    @Test
    fun accessLost_withNoPlanLeft_exitsToStart() = runTest {
        val plans = FakePlanRepository(listOf(mine))
        val current = CurrentPlan()
        val vm = WeeklyViewModel(FakeMealRepository(isAccessible = plans::isAccessible), plans, current, clock, seoul)
        advanceUntilIdle()
        plans.loseAccess(mine.id)
        vm.nextWeek()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.accessLost)

        vm.confirmAccessLost()
        advanceUntilIdle()
        assertEquals(PlanExitDestination.START, vm.uiState.value.exitTo)
        assertNull(current.selected.value)

        // 화면이 이동을 마쳤다고 알리면 이벤트를 지운다. 다시 그려져도 두 번 이동하지 않는다.
        vm.exitHandled()
        assertNull(vm.uiState.value.exitTo)
    }

    @Test
    fun confirmAccessLost_tappedTwice_movesOnce() = runTest {
        val plans = FakePlanRepository(listOf(joined, mine))
        var listCalls = 0
        val counting = object : PlanRepository by plans {
            override suspend fun accessiblePlans(): List<PlanSummary> {
                listCalls++
                delay(100)
                return plans.accessiblePlans()
            }
        }
        val current = CurrentPlan().apply { select(joined) }
        plans.loseAccess(joined.id)
        val vm = WeeklyViewModel(FakeMealRepository(isAccessible = plans::isAccessible), counting, current, clock, seoul)
        advanceUntilIdle()
        listCalls = 0
        vm.confirmAccessLost()
        vm.confirmAccessLost()
        advanceUntilIdle()
        assertEquals(1, listCalls)
        assertEquals(mine, current.selected.value)
    }

    @Test
    fun saveLost_afterSamePlanWasRefreshed_stillShowsNotice() = runTest {
        // 저장하는 사이 같은 식단표의 정보(동반자 유무)만 바뀌어도 같은 식단표다. 안내를 건너뛰면 저장 실패를 모른다.
        val failingSave = object : MealRepository {
            override suspend fun weeklyMeals(planId: PlanId, week: Week): PlanAccess<WeeklyMeals> =
                PlanAccess.Granted(WeeklyMeals(week))

            override suspend fun saveMeal(planId: PlanId, cell: MealCell, text: String?): PlanAccess<Unit> {
                delay(1_000)
                return PlanAccess.Lost
            }
        }
        val current = CurrentPlan().apply { select(mine) }
        val vm = viewModel(meals = failingSave, current = current)
        advanceUntilIdle()
        vm.saveMeal(monLunch, "김치찌개")
        advanceTimeBy(10)
        current.select(mine.copy(hasCompanion = true))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.accessLost)
    }

    /** [slowWeek]를 읽을 때만 1초 늦게, 그 주 첫 칸에 [LATE]를 넣어 돌려준다. */
    private class SlowWeekRepository(private val slowWeek: Week) : MealRepository {
        private val delegate = FakeMealRepository()

        override suspend fun weeklyMeals(planId: PlanId, week: Week): PlanAccess<WeeklyMeals> {
            if (week != slowWeek) return delegate.weeklyMeals(planId, week)
            delay(1_000)
            val empty = WeeklyMeals(week)
            return PlanAccess.Granted(empty.withMenu(empty.cells.first(), LATE))
        }

        override suspend fun saveMeal(planId: PlanId, cell: MealCell, text: String?) =
            delegate.saveMeal(planId, cell, text)
    }

    private companion object {
        const val LATE = "늦은 주"
    }
}
