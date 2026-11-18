# 가짜 저장소와 주간 식단표 화면 (3단계) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 메모리에만 데이터를 두는 가짜 저장소 위에서, 7일 × 3끼 주간 식단표를 보고 주를 오가며 끼니를 입력/수정/삭제하고, "내 식단표"와 "참여 중인 식단표"를 전환할 수 있게 한다.

**Architecture:** 저장소는 인터페이스(`MealRepository`, `PlanRepository`)로 두고 지금은 가짜 구현을 끼운다. 서버가 나오면 구현만 바꾼다. 화면 상태는 화면마다 `ViewModel` + `StateFlow`로 두고, 지금 보고 있는 식단표는 두 화면이 함께 보는 `CurrentPlan`이 들고 있다. 객체 연결은 `AppContainer`에서 손으로 한다(DI 도구 없음). `WeeklyRoute`는 인자 없는 `data object` 그대로 쓴다.

**Tech Stack:** Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material3, JetBrains navigation-compose, lifecycle-viewmodel-compose, kotlinx-coroutines(+ `kotlinx-coroutines-test`), kotlinx-datetime, kotlin.test

**Spec:** `docs/plan.md` 3단계. 화면과 이동은 `docs/screen-flow.md`, 기능 규칙은 `docs/app-developer-brief.md` 4.4장.

## Global Constraints

- 2단계 타입을 그대로 쓴다: `Week`, `MealSlot`, `MealCell`, `WeeklyMeals`, `validateMealText`, `MAX_MEAL_TEXT_LENGTH`. 경로는 `WeeklyRoute`(data object) 그대로.
- 끼니 텍스트 최대 **50자**는 `MAX_MEAL_TEXT_LENGTH`로만 참조한다. 숫자 50을 다른 곳에 적지 않는다.
- 한 주는 월요일 시작. "오늘"은 기기 시간대 기준(`Week.current()` 기본값).
- 식단표 이름은 없다. 목록과 제목은 소유자면 **"내 식단표"**, 동반자면 **"참여 중인 식단표"**.
- 소유자와 동반자의 끼니 편집 권한은 같다. 이 단계에서 역할에 따라 달라지는 것은 표시 이름뿐이다.
- 가짜 저장소는 메모리에만 둔다. 앱 프로세스가 끝나면 사라진다.
- Koin, Ktor를 넣지 않는다. 새 의존성은 `gradle/libs.versions.toml`에 추가한다.
- 공통 코드는 `shared/src/commonMain`, 테스트는 `commonTest`. 명령은 PowerShell(`.\gradlew.bat`). iOS 빌드는 하지 않는다.
- 사용자는 KMP가 처음이다. 각 Task의 "설명할 개념"을 작업 전에 짧게 설명한다.
- 커밋 메시지는 기존 기록처럼 한국어로 쓴다.

## 이 계획에서 정한 것 (사용자 확인 필요)

| 항목 | 결정 | 이유 |
|---|---|---|
| "50자를 넘으면 입력을 막고 안내" | 글자는 계속 칠 수 있게 두고, 50자를 넘으면 **저장 버튼을 끄고** 빨간 안내(`50자까지 적을 수 있어요`)와 `N/50` 글자 수를 보여 준다 | 입력 자체를 잘라 내면 한글 입력기 조합 중에 글자가 깨지고, 붙여 넣은 긴 글이 말없이 잘린다 |
| 공백만 있는 입력을 저장 | 그 칸을 **삭제**한다 | `validateMealText`가 `Empty`를 돌려주는 경우. 빈 메뉴를 따로 저장할 이유가 없다 |
| 식단표를 전환했을 때 보는 주 | **보던 주를 유지**한다 | 두 식단표의 같은 주를 비교하기 쉽다 |
| 앱 첫 진입 시 선택되는 식단표 | 소유한 식단표가 있으면 그것, 없으면 목록의 첫 번째 | "내 식단표"가 기본 |
| 표 모양 | 행 = 요일 7개, 열 = 아침/점심/저녁 | 세로 화면에서 7열은 너무 좁다 |

## 이번에 하지 않는 것

- 저장소 에러 처리(접근 상실, 네트워크 실패). 가짜는 실패하지 않는다. 접근 상실은 설정/연결 해제를 만드는 단계에서 다룬다.
- 식단표 만들기, 시작 화면, 공유 코드 입력의 실제 동작 (4단계 이후). 해당 화면은 지금 placeholder 그대로 둔다.
- 식단표 전환 화면의 "내 식단표 만들기" 버튼. 가짜 데이터에는 항상 소유한 식단표가 있다.
- 자정을 넘겨 앱을 켜 둔 경우 "오늘" 갱신.

## Review Focus

1. 이전/다음 주 버튼을 빠르게 연타하면, 화면에는 **마지막으로 고른 주의 데이터만** 보여야 한다. 늦게 끝난 이전 요청이 덮어쓰면 안 된다 (Task 2 테스트 `rapidWeekChange_showsOnlyLatestWeek`).
2. 끼니 텍스트 경계: 앞뒤 공백 + 50자는 저장되고(공백은 지워짐), 51자는 저장되지 않고 편집 창이 열린 채 남는다. 공백만 저장하면 그 칸이 지워진다 (Task 2 테스트).
3. 한 식단표에 적은 메뉴가 다른 식단표나 다른 주에 섞여 보이면 안 된다 (Task 1 테스트 `plansAndWeeks_areIsolated`, Task 2 테스트 `planChange_reloadsSameWeek`).
4. 화면을 회전해도 보던 주와 입력한 끼니가 유지돼야 한다. 그래서 `AppContainer`는 Compose `remember`가 아니라 프로세스 단위로 하나만 만든다 (Task 3 수동 확인).
5. 식단표 전환 화면에서 항목을 빠르게 두 번 누르면 주간 식단표보다 더 뒤로(앱 종료) 가면 안 된다. `popBackStack<WeeklyRoute>(inclusive = false)`를 쓴다 (Task 4 수동 확인).

---

### Task 1: 식단표 타입, 저장소 인터페이스와 가짜 구현

**설명할 개념:** 인터페이스 뒤에 가짜와 진짜 구현을 바꿔 끼우는 구조, `suspend` 함수(코루틴에서 기다릴 수 있는 함수), `StateFlow`(현재 값을 들고 있고 바뀌면 알려 주는 흐름), `runTest`

**Files:**
- Modify: `gradle/libs.versions.toml` (`kotlinx-coroutines-test` = `org.jetbrains.kotlinx:kotlinx-coroutines-test`)
- Modify: `shared/build.gradle.kts` (`commonTest.dependencies`에 추가)
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/domain/Plan.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/data/MealRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/data/PlanRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/data/FakeMealRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/data/FakePlanRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/data/CurrentPlan.kt`
- Test: `shared/src/commonTest/kotlin/com/mg/bubumeal/data/FakeMealRepositoryTest.kt`
- Test: `shared/src/commonTest/kotlin/com/mg/bubumeal/data/CurrentPlanTest.kt`

**Interfaces:**
- Consumes: `Week`, `MealCell`, `WeeklyMeals` (2단계)
- Produces:
  - `domain/Plan.kt`: `@JvmInline value class PlanId(val value: String)`, `enum class PlanRole { OWNER, COMPANION }` (선언 순서 = 목록 순서), `data class PlanSummary(val id: PlanId, val role: PlanRole)`
  - `interface PlanRepository { suspend fun accessiblePlans(): List<PlanSummary> }` — 소유 + 참여 식단표. 순서는 보장하지 않는다(정렬은 쓰는 쪽에서).
  - `interface MealRepository { suspend fun weeklyMeals(planId: PlanId, week: Week): WeeklyMeals; suspend fun saveMeal(planId: PlanId, cell: MealCell, text: String?) }` — 데이터 없는 주는 빈 `WeeklyMeals(week)`. `text == null`이면 그 칸 삭제. 텍스트 검증은 호출하는 쪽 책임.
  - `class FakePlanRepository(private val plans: List<PlanSummary>) : PlanRepository`
  - `class FakeMealRepository(initial: Map<PlanId, Map<MealCell, String>> = emptyMap()) : MealRepository`
  - `class CurrentPlan { val selected: StateFlow<PlanSummary?>; fun select(plan: PlanSummary) }` — 처음 값은 `null`.

- [ ] **Step 1: 의존성 추가.** `kotlinx-coroutines-test`의 버전은 이미 들어와 있는 `kotlinx-coroutines-core`와 같은 버전으로 한다. `.\gradlew.bat :shared:dependencies --configuration commonMainCompileClasspath`(이름이 다르면 `:shared:dependencies` 출력에서 찾는다)로 core 버전을 확인한 뒤 추가하고, `.\gradlew.bat :shared:compileKotlinMetadata`가 `BUILD SUCCESSFUL`인지 본다.

- [ ] **Step 2: 실패하는 테스트 작성**

```kotlin
// data/FakeMealRepositoryTest.kt
class FakeMealRepositoryTest {
    private val planA = PlanId("a")
    private val planB = PlanId("b")
    private val week = Week(LocalDate(2026, 11, 16))
    private val monLunch = MealCell(LocalDate(2026, 11, 16), MealSlot.LUNCH)

    @Test
    fun weekWithoutData_isEmpty() = runTest {
        val meals = FakeMealRepository().weeklyMeals(planA, week)
        assertEquals(week, meals.week)
        assertTrue(meals.isEmpty())
    }

    @Test
    fun saveThenLoad_returnsMenu() = runTest {
        val repo = FakeMealRepository()
        repo.saveMeal(planA, monLunch, "김치찌개")
        assertEquals("김치찌개", repo.weeklyMeals(planA, week).menuOf(monLunch))
    }

    @Test
    fun saveNull_deletesMenu() = runTest {
        val repo = FakeMealRepository(mapOf(planA to mapOf(monLunch to "김치찌개")))
        repo.saveMeal(planA, monLunch, null)
        assertTrue(repo.weeklyMeals(planA, week).isEmpty())
    }

    @Test
    fun plansAndWeeks_areIsolated() = runTest {
        val repo = FakeMealRepository()
        repo.saveMeal(planA, monLunch, "김치찌개")
        assertTrue(repo.weeklyMeals(planB, week).isEmpty())
        assertTrue(repo.weeklyMeals(planA, week.next()).isEmpty())
        assertTrue(repo.weeklyMeals(planA, week.previous()).isEmpty())
    }
}

// data/CurrentPlanTest.kt
class CurrentPlanTest {
    @Test
    fun startsEmpty_thenHoldsSelection() {
        val current = CurrentPlan()
        assertNull(current.selected.value)
        val plan = PlanSummary(PlanId("a"), PlanRole.OWNER)
        current.select(plan)
        assertEquals(plan, current.selected.value)
    }
}
```

- [ ] **Step 3: 실패 확인.** Run: `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.data.*"` Expected: FAIL. 컴파일 오류(`PlanId`, `FakeMealRepository` 등이 없음)
- [ ] **Step 4: 구현.** 위 Interfaces대로 만든다. `FakeMealRepository`는 `MutableMap<PlanId, MutableMap<MealCell, String>>`에 저장하고, `weeklyMeals`는 날짜가 `week`에 드는 칸만 골라 `WeeklyMeals(week, ...)`로 돌려준다. `CurrentPlan`은 `MutableStateFlow`를 감싸 읽기 전용 `StateFlow`로 내보낸다. 각 파일 맨 위에 "서버가 나오면 무엇으로 바뀌는지" 한 줄 주석을 단다.
- [ ] **Step 5: 통과 확인.** Run: `.\gradlew.bat :shared:testAndroidHostTest` Expected: PASS. 2단계 테스트 포함 전부 통과
- [ ] **Step 6: Commit**

```powershell
git add gradle/libs.versions.toml shared/build.gradle.kts shared/src/commonMain/kotlin/com/mg/bubumeal/domain/Plan.kt shared/src/commonMain/kotlin/com/mg/bubumeal/data shared/src/commonTest/kotlin/com/mg/bubumeal/data
git commit -m "식단표 타입과 가짜 저장소 추가"
```

---

### Task 2: 주간 식단표 ViewModel

**설명할 개념:** `ViewModel`(화면 회전에도 살아남는 상태 보관소), `viewModelScope`와 `launch`, `combine` + `collectLatest`(새 값이 오면 진행 중인 이전 작업을 취소), 테스트에서 `Dispatchers.setMain`으로 메인 스레드를 바꾸는 이유

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/weekly/WeeklyViewModel.kt`
- Test: `shared/src/commonTest/kotlin/com/mg/bubumeal/ui/weekly/WeeklyViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `MealRepository`, `PlanRepository`, `CurrentPlan`, `PlanSummary`, `PlanRole`; 2단계의 `Week`, `WeeklyMeals`, `MealCell`, `validateMealText`
- Produces:

```kotlin
data class WeeklyUiState(
    val plan: PlanSummary?,
    val meals: WeeklyMeals,
    val today: LocalDate,
    val isLoading: Boolean,
    val hasNoPlan: Boolean = false,
    val editingCell: MealCell? = null,
) {
    val week: Week get() = meals.week
    val isCurrentWeek: Boolean get() = today in week
    /** 로딩이 끝났고 식단표가 있는데 이 주에 메뉴가 하나도 없을 때. */
    val showEmptyHint: Boolean get() = !isLoading && plan != null && meals.isEmpty()
}

class WeeklyViewModel(
    mealRepository: MealRepository,
    planRepository: PlanRepository,
    currentPlan: CurrentPlan,
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    val uiState: StateFlow<WeeklyUiState>
    fun previousWeek()
    fun nextWeek()
    fun goToCurrentWeek()
    fun openEditor(cell: MealCell)
    fun closeEditor()
    /** 검증 결과: Valid → 저장 후 편집 창 닫기, Empty → 삭제 후 닫기, TooLong → 아무것도 하지 않음(창 유지). */
    fun saveMeal(cell: MealCell, input: String)
    fun deleteMeal(cell: MealCell)
}
```

- [ ] **Step 1: 실패하는 테스트 작성.** 공통 준비:

```kotlin
class WeeklyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    // 2026-11-18(수) 서울 낮
    private val clock = object : Clock { override fun now() = Instant.parse("2026-11-18T03:00:00Z") }
    private val seoul = TimeZone.of("Asia/Seoul")
    private val thisWeek = Week(LocalDate(2026, 11, 16))
    private val mine = PlanSummary(PlanId("mine"), PlanRole.OWNER)
    private val joined = PlanSummary(PlanId("joined"), PlanRole.COMPANION)
    private val monLunch = MealCell(LocalDate(2026, 11, 16), MealSlot.LUNCH)

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        meals: MealRepository = FakeMealRepository(),
        plans: List<PlanSummary> = listOf(joined, mine), // 일부러 참여 식단표를 먼저 둔다
        current: CurrentPlan = CurrentPlan(),
    ) = WeeklyViewModel(meals, FakePlanRepository(plans), current, clock, seoul)
```

테스트 (모두 `runTest { ...; advanceUntilIdle(); ... }` 형태):

| 이름 | 준비 / 동작 | 확인 |
|---|---|---|
| `init_selectsOwnedPlanAndLoadsCurrentWeek` | 기본 | `plan == mine`, `week == thisWeek`, `isLoading == false`, `isCurrentWeek`, `current.selected.value == mine` |
| `init_keepsAlreadySelectedPlan` | `current`에 미리 `joined` 선택 | `plan == joined` |
| `init_withNoPlans_setsHasNoPlan` | `plans = emptyList()` | `hasNoPlan`, `plan == null`, `!showEmptyHint` |
| `emptyWeek_showsEmptyHint` | 기본 | `showEmptyHint`, `meals.cells.size == 21` |
| `nextAndPrevious_changeWeek` | `nextWeek()` → 확인 → `previousWeek()` 두 번 | `week == thisWeek.next()`, 그다음 `thisWeek.previous()`, `!isCurrentWeek` |
| `goToCurrentWeek_returnsToToday` | `nextWeek()` 두 번 → `goToCurrentWeek()` | `week == thisWeek` |
| `saveMeal_persistsAcrossWeekNavigation` | `saveMeal(monLunch, "김치찌개")` → `nextWeek()` → `previousWeek()` | `meals.menuOf(monLunch) == "김치찌개"`, `!showEmptyHint` |
| `saveMeal_trimsAndAccepts50Chars` | `openEditor(monLunch)`; `saveMeal(monLunch, "  " + "가".repeat(MAX_MEAL_TEXT_LENGTH) + " ")` | 메뉴 == `"가".repeat(MAX_MEAL_TEXT_LENGTH)`, `editingCell == null` |
| `saveMeal_tooLong_isRejectedAndEditorStaysOpen` | `openEditor(monLunch)`; `saveMeal(monLunch, "가".repeat(MAX_MEAL_TEXT_LENGTH + 1))` | `menuOf(monLunch) == null`, `editingCell == monLunch`, 저장소에서 다시 읽어도 없음 |
| `saveMeal_blank_deletesMenu` | 저장소에 `monLunch → "김치찌개"`; `saveMeal(monLunch, "   ")` | `menuOf(monLunch) == null` |
| `deleteMeal_removesMenuAndClosesEditor` | 저장소에 메뉴; `openEditor` → `deleteMeal(monLunch)` | 메뉴 없음, `editingCell == null` |
| `openAndCloseEditor` | `openEditor(monLunch)` → `closeEditor()` | `monLunch` → `null` |
| `planChange_reloadsSameWeek` | 저장소에 `joined`의 `monLunch → "카레"`; `nextWeek()`; `previousWeek()`; `current.select(joined)` | `plan == joined`, `week == thisWeek`, `menuOf(monLunch) == "카레"`; 다시 `select(mine)` 하면 `null` |
| `rapidWeekChange_showsOnlyLatestWeek` | 아래 `SlowFirstLoadRepository` 사용, `nextWeek()` 직후 `advanceTimeBy(10)` → `nextWeek()` → `advanceUntilIdle()` | `week == thisWeek.next().next()`, `meals.week == week`, `menuOf`가 다음다음 주 데이터(`"늦은 주"`가 아님) |

`rapidWeekChange` 테스트용 가짜 (테스트 파일 안에 둔다): `FakeMealRepository`를 감싸 `thisWeek.next()`를 읽을 때만 `delay(1_000)`하고, 그 주의 아무 칸에 `"늦은 주"`를 돌려준다. 다음다음 주는 바로 돌려준다. 이전 로딩이 취소되지 않으면 1초 뒤 `"늦은 주"`가 화면 상태를 덮어써 실패한다.

- [ ] **Step 2: 실패 확인.** Run: `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.ui.weekly.WeeklyViewModelTest"` Expected: FAIL. 컴파일 오류(`WeeklyViewModel` 없음)
- [ ] **Step 3: 구현.** 접근:
  - 보는 주는 `MutableStateFlow<Week>`(처음 값 `Week.current(clock, timeZone)`), 상태는 `MutableStateFlow<WeeklyUiState>`.
  - `init`에서 (1) `currentPlan.selected.value == null`이면 `accessiblePlans()`를 받아 `sortedBy { it.role }`의 첫 번째를 `select`, 비어 있으면 `hasNoPlan = true, isLoading = false`. (2) `combine(currentPlan.selected.filterNotNull(), week)`를 `collectLatest`로 모아, 시작할 때 `plan`/`meals = WeeklyMeals(week)`/`isLoading = true`로 바꾸고 `weeklyMeals` 결과로 채운다. `collectLatest`가 늦게 끝나는 이전 로딩을 취소한다.
  - `saveMeal`/`deleteMeal`은 저장소에 쓴 뒤, 그사이 식단표나 주가 바뀌지 않았을 때만 `meals.withMenu(cell, text)`로 상태를 고친다.
- [ ] **Step 4: 통과 확인.** Run: `.\gradlew.bat :shared:testAndroidHostTest` Expected: PASS. 전체 통과
- [ ] **Step 5: Commit**

```powershell
git add shared/src/commonMain/kotlin/com/mg/bubumeal/ui/weekly/WeeklyViewModel.kt shared/src/commonTest/kotlin/com/mg/bubumeal/ui/weekly
git commit -m "주간 식단표 ViewModel 추가"
```

---

### Task 3: 주간 식단표 화면과 끼니 편집 창

**설명할 개념:** `collectAsStateWithLifecycle`(StateFlow를 Compose 상태로 바꾸기), `viewModel { ... }`(NavHost 화면마다 ViewModel 하나, 화면 회전에도 유지), 상태 끌어올리기(화면은 상태를 받아 그리기만 하고 이벤트는 람다로 올려 보냄), 화면 회전과 `remember`의 수명 차이

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/Labels.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/weekly/WeeklyScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/weekly/MealEditDialog.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/AppContainer.kt`
- Modify: `shared/src/commonMain/kotlin/com/mg/bubumeal/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/mg/bubumeal/navigation/AppNavHost.kt` (`composable<WeeklyRoute>`만 교체)
- Test: `shared/src/commonTest/kotlin/com/mg/bubumeal/ui/LabelsTest.kt`

**Interfaces:**
- Consumes: Task 1의 모든 타입, Task 2의 `WeeklyViewModel`, `WeeklyUiState`
- Produces:
  - `ui/Labels.kt`: `fun PlanRole.label(): String`, `fun MealSlot.label(): String`, `fun dayLabel(date: LocalDate): String`, `fun weekRangeLabel(week: Week): String`, `fun mealCellTitle(cell: MealCell): String`
  - `class AppContainer(clock: Clock = Clock.System, timeZone: TimeZone = TimeZone.currentSystemDefault()) { val planRepository: PlanRepository; val mealRepository: MealRepository; val currentPlan: CurrentPlan }`
  - `@Composable fun AppNavHost(container: AppContainer, navController: NavHostController = rememberNavController())`
  - `@Composable fun WeeklyScreen(state: WeeklyUiState, onPreviousWeek, onNextWeek, onCurrentWeek, onCellClick: (MealCell) -> Unit, onSwitchPlan, onOpenSettings, onSave: (MealCell, String) -> Unit, onDelete: (MealCell) -> Unit, onDismissEditor)` (모든 콜백은 `() -> Unit`이고 인자가 있는 것만 표기)

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
class LabelsTest {
    @Test fun planRoleLabels() {
        assertEquals("내 식단표", PlanRole.OWNER.label())
        assertEquals("참여 중인 식단표", PlanRole.COMPANION.label())
    }
    @Test fun mealSlotLabels() =
        assertEquals(listOf("아침", "점심", "저녁"), MealSlot.entries.map { it.label() })
    @Test fun dayLabel_hasMonthDayAndWeekday() {
        assertEquals("11/16 (월)", dayLabel(LocalDate(2026, 11, 16)))
        assertEquals("11/22 (일)", dayLabel(LocalDate(2026, 11, 22)))
    }
    @Test fun weekRange_sameYear() =
        assertEquals("11월 16일 ~ 11월 22일", weekRangeLabel(Week(LocalDate(2026, 11, 16))))
    @Test fun weekRange_acrossYears_showsYears() =
        assertEquals("2026년 12월 28일 ~ 2027년 1월 3일", weekRangeLabel(Week(LocalDate(2026, 12, 28))))
    @Test fun mealCellTitle() =
        assertEquals("11/16 (월) 저녁", mealCellTitle(MealCell(LocalDate(2026, 11, 16), MealSlot.DINNER)))
}
```

- [ ] **Step 2: 실패 확인.** Run: `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.ui.LabelsTest"` Expected: FAIL. 컴파일 오류
- [ ] **Step 3: `Labels.kt` 구현.** 요일은 `dayOfWeek.isoDayNumber`로 `"월화수목금토일"`에서 고른다.
- [ ] **Step 4: 통과 확인.** 같은 명령. Expected: PASS
- [ ] **Step 5: 화면 구현.**
  - `AppContainer`: 가짜 식단표 두 개 `PlanSummary(PlanId("my-plan"), OWNER)`, `PlanSummary(PlanId("joined-plan"), COMPANION)`. 전환해 보면 차이가 보이도록 `joined-plan`의 이번 주 월요일 점심 `"김치찌개"`, 수요일 저녁 `"카레"`를 미리 넣는다. 내 식단표는 비워 둔다.
  - `App.kt`: `AppContainer`는 파일 최상위 `private val appContainer by lazy { AppContainer() }`로 프로세스에 하나만 둔다. `remember`에 두면 화면 회전 때 Activity가 다시 만들어지면서 가짜 데이터가 사라진다는 주석을 단다 (Review Focus 4). `AppNavHost(appContainer)`를 부른다.
  - `AppNavHost`: `composable<WeeklyRoute>`에서 `viewModel { WeeklyViewModel(container.mealRepository, container.planRepository, container.currentPlan) }`로 만들고 `uiState`를 `collectAsStateWithLifecycle()`로 읽어 `WeeklyScreen`에 넘긴다. 전환은 `PlanSwitchRoute`, 설정은 `PlanSettingsRoute`로 기존 `navigateFrom`을 써서 이동한다. 다른 화면은 그대로 둔다.
  - `WeeklyScreen` (위에서 아래로):
    - 상단 바(`TopAppBar`): 제목 = `state.plan?.role?.label()`. 오른쪽에 텍스트 버튼 `전환`, `설정`.
    - 주 이동 줄: `◀` 버튼, 가운데 `weekRangeLabel(state.week)`, `▶` 버튼. `!isCurrentWeek`일 때만 그 아래 `이번 주로` 버튼.
    - `showEmptyHint`일 때 안내 문구: `이 주에는 아직 식단이 없어요. 칸을 눌러 메뉴를 적어 보세요.`
    - 표: 머리 행(빈칸, 아침, 점심, 저녁) + 요일 7행. 첫 열은 `dayLabel`, 오늘 행은 `MaterialTheme.colorScheme.primaryContainer` 배경. 칸은 누르면 `onCellClick`, 메뉴가 있으면 최대 3줄 + 말줄임, 없으면 흐린 `+`. 세로 스크롤 가능.
    - `hasNoPlan`이면 표 대신 `식단표가 없어요.`만 보인다. `isLoading`이면 표 위에 작은 진행 표시.
    - `editingCell != null`이면 `MealEditDialog`.
  - `MealEditDialog(cell, initialText: String?, onSave: (String) -> Unit, onDelete: () -> Unit, onDismiss)`: 제목 `mealCellTitle(cell)`. 입력값은 `rememberSaveable(cell) { mutableStateOf(initialText ?: "") }`로 창 안에 둔다(ViewModel의 StateFlow로 TextField 값을 돌리면 한글 조합 중 커서가 튄다). 매 입력마다 `validateMealText`를 불러 아래 글자 수 `N/$MAX_MEAL_TEXT_LENGTH`(N = 앞뒤 공백 뺀 길이)를 보여 주고, `TooLong`이면 `isError = true`, 안내 `${MAX_MEAL_TEXT_LENGTH}자까지 적을 수 있어요`, `저장` 버튼 끔. 버튼: `저장`, `취소`, 기존 메뉴가 있을 때만 `삭제`.
- [ ] **Step 6: 빌드 확인.** Run: `.\gradlew.bat :androidApp:assembleDebug` 와 `.\gradlew.bat :shared:testAndroidHostTest` Expected: 둘 다 `BUILD SUCCESSFUL`
- [ ] **Step 7: 에뮬레이터 확인 (사용자에게 부탁).** `.\gradlew.bat :androidApp:installDebug` 뒤 로그인 → 식단표 만들기로 들어가서:
  - 빈 주 안내가 보이고, 오늘 행이 강조된다.
  - 칸을 눌러 메뉴를 적고 저장 → 다음 주로 갔다가 돌아와도 남아 있다 (plan.md 3단계 확인 방법).
  - 51자를 넣으면 빨간 안내가 뜨고 저장 버튼이 꺼진다. 메뉴를 지우고 저장하거나 `삭제`를 누르면 칸이 빈다.
  - 화면을 회전해도 보던 주, 입력한 메뉴, 편집 중인 글이 남아 있다 (Review Focus 4).
- [ ] **Step 8: Commit**

```powershell
git add shared/src/commonMain/kotlin/com/mg/bubumeal shared/src/commonTest/kotlin/com/mg/bubumeal/ui/LabelsTest.kt
git commit -m "주간 식단표 화면과 끼니 편집 창 추가"
```

---

### Task 4: 식단표 전환 화면과 문서 정리

**설명할 개념:** 여러 화면이 같은 상태(`CurrentPlan`)를 보는 방식. 경로 인자 대신 공유 상태로 "어떤 식단표를 보는지"를 넘기는 이유(`WeeklyRoute`를 그대로 두기 위해), 서버가 생기면 이 자리를 무엇이 채울지

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/planswitch/PlanSwitchViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/planswitch/PlanSwitchScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mg/bubumeal/navigation/AppNavHost.kt` (`composable<PlanSwitchRoute>`만 교체)
- Modify: `CLAUDE.md` (아키텍처의 `commonMain` 목록에 `data/` 추가, `ui/` 설명 갱신, `AppContainer` 한 줄)
- Test: `shared/src/commonTest/kotlin/com/mg/bubumeal/ui/planswitch/PlanSwitchViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `PlanRepository`, `CurrentPlan`, `PlanSummary`; Task 3의 `PlanRole.label()`, `AppContainer`
- Produces:

```kotlin
data class PlanSwitchUiState(
    val plans: List<PlanSummary> = emptyList(), // 소유 먼저 (PlanRole 순서)
    val selectedId: PlanId? = null,
    val isLoading: Boolean = true,
)
class PlanSwitchViewModel(planRepository: PlanRepository, currentPlan: CurrentPlan) : ViewModel() {
    val uiState: StateFlow<PlanSwitchUiState>
    fun select(plan: PlanSummary)
}
@Composable fun PlanSwitchScreen(state: PlanSwitchUiState, onSelect: (PlanSummary) -> Unit, onEnterCode: () -> Unit, onBack: () -> Unit)
```

- [ ] **Step 1: 실패하는 테스트 작성** (Task 2와 같은 `setMain` 준비, `mine`/`joined` 동일)

| 이름 | 준비 / 동작 | 확인 |
|---|---|---|
| `plans_ownedFirst` | 저장소 순서 `[joined, mine]` | `plans == listOf(mine, joined)`, `!isLoading` |
| `selectedId_followsCurrentPlan` | `current.select(joined)` 후 생성 | `selectedId == joined.id` |
| `select_updatesCurrentPlan` | `select(joined)` | `current.selected.value == joined`, `uiState.selectedId == joined.id` |

- [ ] **Step 2: 실패 확인.** Run: `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.ui.planswitch.*"` Expected: FAIL. 컴파일 오류
- [ ] **Step 3: 구현.** `uiState`는 저장소에서 받은 목록과 `currentPlan.selected`를 `combine`해 만든다. 화면: 상단 바 제목 `식단표 전환`(뒤로 버튼), 목록 각 행에 `plan.role.label()`과 선택된 행에 체크 표시, 목록 아래 `공유 코드 입력` 버튼.
  - `AppNavHost`: 행을 누르면 `vm.select(plan)` 뒤 `navController.popBackStack<WeeklyRoute>(inclusive = false)` (두 번 눌러도 주간 식단표에서 멈춘다, Review Focus 5). `공유 코드 입력`은 `navigateFrom(entry, CodeInputRoute)`. 뒤로 버튼은 같은 `popBackStack<WeeklyRoute>(inclusive = false)`.
- [ ] **Step 4: 통과 확인.** Run: `.\gradlew.bat :shared:testAndroidHostTest` 와 `.\gradlew.bat :androidApp:assembleDebug` Expected: 둘 다 `BUILD SUCCESSFUL`
- [ ] **Step 5: 에뮬레이터 확인 (사용자에게 부탁).**
  - 처음 제목은 `내 식단표`. `전환` → `참여 중인 식단표` 선택 → 제목이 바뀌고 이번 주에 김치찌개/카레가 보인다. 보던 주가 유지된다.
  - 다시 `내 식단표`로 바꾸면 Task 3에서 적은 메뉴가 그대로 있고, 참여 식단표 메뉴는 섞이지 않는다.
  - 목록 항목을 빠르게 두 번 눌러도 앱이 닫히지 않는다 (Review Focus 5).
- [ ] **Step 6: `CLAUDE.md` 갱신.** `commonMain` 목록에 `data/`(저장소 인터페이스, 지금은 `Fake*` 구현, 서버 연동 시 교체), `ui/`에 `weekly/`, `planswitch/`, `AppContainer`(손으로 객체 연결, 프로세스에 하나)를 추가한다. `PlaceholderScreen`은 아직 남은 화면용이라고 고친다.
- [ ] **Step 7: Commit**

```powershell
git add shared/src/commonMain/kotlin/com/mg/bubumeal shared/src/commonTest/kotlin/com/mg/bubumeal/ui/planswitch CLAUDE.md
git commit -m "식단표 전환 화면 추가와 CLAUDE.md 갱신"
```
