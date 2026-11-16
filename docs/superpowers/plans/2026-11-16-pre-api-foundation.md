# 서버 없이 하는 기초 작업 (0~2단계) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 화면 흐름 문서를 만들고, 빈 화면 사이를 오가는 내비게이션 뼈대와 식단 도메인 기본 타입(주 계산, 끼니 텍스트 검증, 한 주 식단)을 테스트와 함께 만든다.

**Architecture:** 모든 코드는 `shared/src/commonMain`에 둔다. 화면 경로는 `@Serializable` 타입으로 정의하고 JetBrains `navigation-compose`의 `NavHost` 하나로 이동한다. 도메인 타입은 UI와 무관한 순수 Kotlin(`com.mg.bubumeal.domain`)이며 `kotlinx-datetime`의 `LocalDate`를 쓴다.

**Tech Stack:** Kotlin 2.4.21, Compose Multiplatform 1.12.2, JetBrains navigation-compose, kotlinx-serialization, kotlinx-datetime, kotlin.test

**Spec:** `docs/plan.md` (0~2단계). 기능 규칙은 `docs/app-developer-brief.md`. 3~5단계는 이 계획이 끝난 뒤 별도 계획으로 쓴다.

## Global Constraints

- 한 주는 **월요일**에 시작하고 일요일에 끝난다.
- 끼니 텍스트 최대 길이 **50자**. 상수 `MAX_MEAL_TEXT_LENGTH` 하나로만 정의한다.
- "오늘"은 **기기 시간대**(`TimeZone.currentSystemDefault()`) 기준. 임시 결정이므로 시간대를 인자로 받아 바꿀 수 있게 한다.
- 공통 로직과 UI는 `shared/src/commonMain`, 플랫폼별 코드는 `expect`/`actual`.
- 새 의존성은 `gradle/libs.versions.toml`에 버전과 함께 추가한다. Koin, Ktor는 넣지 않는다.
- 버전은 Kotlin 2.4.21 / Compose Multiplatform 1.12.2와 호환되는 최신 안정판을 고르고, 빌드가 통과하는지로 확인한다.
- 명령은 PowerShell 기준(`.\gradlew.bat`). iOS 빌드는 실행하지 않는다. 테스트는 `:shared:testAndroidHostTest`로 확인한다.
- 사용자는 KMP가 처음이다. 각 Task의 "설명할 개념"을 작업 전에 짧게 설명한다.

## Review Focus

1. 로그인 뒤 뒤로 가기를 누르면 로그인 화면으로 돌아가지 않고 앱이 닫혀야 한다. 공유 코드 입력에 성공한 뒤에도 입력 화면으로 돌아가지 않아야 한다 (Task 2 수동 확인).
2. 연말과 연초에 걸친 주(2026-12-28 ~ 2027-01-03)가 한 주로 묶여야 한다 (Task 3 테스트).
3. 자정 근처에서는 시간대에 따라 "이번 주"가 달라진다. 서울 기준 월요일 00:30은 이미 새로운 주여야 한다 (Task 3 테스트).
4. 끼니 텍스트 글자 수는 `String.length`(UTF-16 단위)로 센다. 이모지 `🍚`는 2글자라 25개는 통과하고 26개는 실패한다 (Task 4 테스트). (2026-11-16 사용자 결정으로 "눈에 보이는 한 글자" 기준에서 변경)
5. 앞뒤 공백만 있는 끼니 텍스트는 "빈 값"으로 처리한다. 가운데 줄바꿈은 그대로 둔다 (Task 4 테스트).

---

### Task 1: 화면 흐름 문서

**설명할 개념:** 없음 (문서 작업)

**Files:**
- Create: `docs/screen-flow.md`

**Interfaces:**
- Produces: Task 2가 쓰는 화면 목록과 경로 이름. 아래 표의 이름을 그대로 쓴다.

| 화면 | 경로 타입 | 진입 방법 | 다루는 에러 (기획 5장) |
|---|---|---|---|
| 로그인 | `LoginRoute` | 앱 시작 | 사용자 취소, 토큰 검증 실패, 네트워크/제공자 오류 |
| 시작 (식단표 없음) | `StartRoute` | 로그인 후 접근 가능한 식단표가 없을 때 | 없음 |
| 주간 식단표 | `WeeklyRoute` | 만들기, 코드 입력 성공, 로그인 후 식단표가 있을 때 | 접근 상실 (연결 해제/삭제됨) |
| 공유 코드 입력 | `CodeInputRoute` | 시작 화면, 식단표 전환 화면 | 형식 오류, 만료, 이미 사용됨, 이미 동반자 있음, 본인 코드, 15분 차단 |
| 식단표 전환 | `PlanSwitchRoute` | 주간 식단표 | 없음 |
| 식단표 설정 | `PlanSettingsRoute` | 주간 식단표 | 권한 없음 (동반자가 발급/삭제 시도) |
| 공유 코드 발급 | `IssueCodeRoute` | 식단표 설정 (소유자만 보임) | 권한 없음 |

화면이 아닌 것(대화상자): 끼니 편집(주간 식단표 위), 연결 해제 확인과 식단표 삭제 2차 확인(설정 위).

- [ ] **Step 1:** `docs/screen-flow.md`를 쓴다. 위 표, `docs/plan.md` 0단계의 mermaid 흐름도, 대화상자 목록, 화면마다 소유자와 동반자에게 보이는 차이(설정 화면의 발급/삭제 버튼)를 넣는다.
- [ ] **Step 2:** 사용자에게 문서를 보여 주고 승인받는다. **승인 전에는 Task 2를 시작하지 않는다.** 화면이 바뀌면 Task 2의 경로 목록도 같이 고친다.
- [ ] **Step 3: Commit**

```powershell
git add docs/screen-flow.md
git commit -m "화면 흐름 문서 추가"
```

---

### Task 2: 템플릿 정리와 내비게이션 뼈대

**설명할 개념:** `commonMain`의 Compose UI가 Android와 iOS에서 같이 쓰이는 구조, `libs.versions.toml`과 플러그인 추가, `@Serializable` 경로를 쓰는 타입 안전 내비게이션

**Files:**
- Modify: `gradle/libs.versions.toml` (버전 `navigation-compose`, `kotlinx-serialization`; 라이브러리 `navigation-compose` = `org.jetbrains.androidx.navigation:navigation-compose`, `kotlinx-serialization-core` = `org.jetbrains.kotlinx:kotlinx-serialization-core`; 플러그인 `kotlinSerialization` = `org.jetbrains.kotlin.plugin.serialization`, version.ref `kotlin`)
- Modify: `build.gradle.kts` (`alias(libs.plugins.kotlinSerialization) apply false`)
- Modify: `shared/build.gradle.kts` (플러그인 적용, `commonMain.dependencies`에 두 라이브러리)
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/navigation/Routes.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/navigation/AppNavHost.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/PlaceholderScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mg/bubumeal/App.kt`
- Delete: `shared/src/commonMain/kotlin/com/mg/bubumeal/Greeting.kt`, `GreetingUtil.kt`, `shared/src/commonMain/composeResources/drawable/compose-multiplatform.xml`
- 유지: `Platform.kt`와 `actual` 구현 (5단계 전까지 `expect`/`actual` 예시로 남긴다)

**Interfaces:**
- Consumes: Task 1의 경로 이름
- Produces:
  - `Routes.kt`: `@Serializable data object` 7개 (`LoginRoute`, `StartRoute`, `WeeklyRoute`, `CodeInputRoute`, `PlanSwitchRoute`, `PlanSettingsRoute`, `IssueCodeRoute`). 3단계 이후 인자가 필요하면 그때 `data class`로 바꾼다.
  - `@Composable fun AppNavHost(navController: NavHostController = rememberNavController())`: 시작 경로 `LoginRoute`
  - `@Composable fun PlaceholderScreen(title: String, actions: List<Pair<String, () -> Unit>>)`: 제목 하나와 버튼 목록
  - `App()`은 `MaterialTheme { AppNavHost() }`만 남긴다.

- [ ] **Step 1:** 의존성과 플러그인을 추가하고 `.\gradlew.bat :shared:compileKotlinMetadata`로 의존성이 받아지는지 확인한다. Expected: `BUILD SUCCESSFUL`
- [ ] **Step 2:** Greeting 관련 파일과 drawable을 지우고, `Routes.kt`, `PlaceholderScreen.kt`, `AppNavHost.kt`를 만든 다음 `App.kt`를 바꾼다. 화면 사이의 이동은 아래 표대로 한다.

| 화면 | 버튼 → 이동 | 백스택 처리 |
|---|---|---|
| Login | "가짜 로그인" → Start | `popUpTo<LoginRoute> { inclusive = true }` |
| Start | "식단표 만들기" → Weekly | `popUpTo<StartRoute> { inclusive = true }` |
| Start | "공유 코드 입력" → CodeInput | 없음 |
| CodeInput | "가짜 성공" → Weekly | `popUpTo(navController.graph.id) { inclusive = true }`로 백스택을 모두 비우고 Weekly 하나만 남긴다. 시작 화면에서 왔든 전환 화면에서 왔든 같다 |
| Weekly | "식단표 전환" → PlanSwitch, "설정" → PlanSettings | 없음 |
| PlanSwitch | "공유 코드 입력" → CodeInput | 없음 |
| PlanSettings | "공유 코드 발급" → IssueCode | 없음 |

- [ ] **Step 3:** `.\gradlew.bat :androidApp:assembleDebug`를 실행한다. Expected: `BUILD SUCCESSFUL`
- [ ] **Step 4:** `.\gradlew.bat :shared:testAndroidHostTest`를 실행한다. Expected: `BUILD SUCCESSFUL` (기존 예제 테스트가 계속 통과)
- [ ] **Step 5:** 사용자에게 에뮬레이터 확인을 부탁한다 (`.\gradlew.bat :androidApp:installDebug` 또는 IDE 실행). 다음을 확인한다.
  - 모든 버튼으로 화면을 오갈 수 있다.
  - Weekly에서 뒤로 가기를 누르면 앱이 닫힌다 (Review Focus 1).
  - PlanSwitch → CodeInput → 성공 뒤 뒤로 가기를 눌러도 CodeInput으로 돌아가지 않는다.
- [ ] **Step 6: Commit**

```powershell
git add -A gradle/libs.versions.toml build.gradle.kts shared
git commit -m "내비게이션 뼈대 추가, 템플릿 예제 제거"
```

---

### Task 3: 끼니 종류와 주 계산 (TDD)

**설명할 개념:** `commonTest`(모든 플랫폼에서 도는 테스트)와 `testAndroidHostTest`로 PC에서 바로 실행하는 방법, 테스트를 먼저 쓰는 순서, `kotlinx-datetime`의 `LocalDate`/`TimeZone`/`Clock`

**Files:**
- Modify: `gradle/libs.versions.toml`, `shared/build.gradle.kts` (`kotlinx-datetime` = `org.jetbrains.kotlinx:kotlinx-datetime`를 `commonMain`에)
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/domain/MealSlot.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/domain/Week.kt`
- Create: `shared/src/commonTest/kotlin/com/mg/bubumeal/domain/WeekTest.kt`
- Delete: `shared/src/commonTest/kotlin/com/mg/bubumeal/SharedCommonTest.kt` (예제 테스트)

**Interfaces:**
- Produces:
  - `enum class MealSlot { BREAKFAST, LUNCH, DINNER }`: 선언 순서가 화면 표시 순서다.
  - `data class Week(val start: LocalDate)`: `init`에서 `start.dayOfWeek == DayOfWeek.MONDAY`가 아니면 `IllegalArgumentException`
    - `val end: LocalDate`: 일요일
    - `val days: List<LocalDate>`: 월~일 7개
    - `fun previous(): Week`, `fun next(): Week`
    - `operator fun contains(date: LocalDate): Boolean`
    - `companion object { fun containing(date: LocalDate): Week; fun current(clock: Clock = Clock.System, timeZone: TimeZone = TimeZone.currentSystemDefault()): Week }`
  - `Clock`은 `kotlin.time.Clock`이다 (kotlinx-datetime 0.7 이상). 사용하는 Kotlin 버전에서 실험 기능이면 `@OptIn(ExperimentalTime::class)`을 붙인다.

- [ ] **Step 1: 실패하는 테스트 작성** (`WeekTest`)

```kotlin
@Test fun containing_midweek_returnsMondayToSunday() {
    val week = Week.containing(LocalDate(2026, 11, 18))
    assertEquals(LocalDate(2026, 11, 16), week.start)
    assertEquals(LocalDate(2026, 11, 22), week.end)
}
@Test fun containing_sunday_isLastDayOfWeek() =
    assertEquals(LocalDate(2026, 11, 16), Week.containing(LocalDate(2026, 11, 22)).start)
@Test fun containing_monday_isFirstDay() =
    assertEquals(LocalDate(2026, 11, 16), Week.containing(LocalDate(2026, 11, 16)).start)
@Test fun containing_yearBoundary() {
    val week = Week.containing(LocalDate(2027, 1, 1))
    assertEquals(LocalDate(2026, 12, 28), week.start)
    assertEquals(LocalDate(2027, 1, 3), week.days.last())
}
@Test fun days_areSevenConsecutiveDays() {
    val days = Week(LocalDate(2026, 11, 16)).days
    assertEquals(7, days.size)
    assertEquals((16..22).map { LocalDate(2026, 11, it) }, days)
}
@Test fun previousAndNext() {
    val week = Week(LocalDate(2026, 11, 16))
    assertEquals(LocalDate(2026, 11, 9), week.previous().start)
    assertEquals(LocalDate(2026, 11, 23), week.next().start)
}
@Test fun contains_onlyDatesInWeek() {
    val week = Week(LocalDate(2026, 11, 16))
    assertTrue(LocalDate(2026, 11, 22) in week)
    assertFalse(LocalDate(2026, 11, 23) in week)
}
@Test fun constructor_rejectsNonMonday() {
    assertFailsWith<IllegalArgumentException> { Week(LocalDate(2026, 11, 17)) }
}
@Test fun current_dependsOnTimeZone() {
    // 2026-11-15T15:30Z = 서울 2026-11-16(월) 00:30 = UTC 2026-11-15(일) 15:30
    val clock = object : Clock { override fun now() = Instant.parse("2026-11-15T15:30:00Z") }
    assertEquals(LocalDate(2026, 11, 16), Week.current(clock, TimeZone.of("Asia/Seoul")).start)
    assertEquals(LocalDate(2026, 11, 9), Week.current(clock, TimeZone.UTC).start)
}
@Test fun mealSlot_order() =
    assertEquals(listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER), MealSlot.entries)
```

- [ ] **Step 2: 실패 확인**

Run: `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.domain.WeekTest"`
Expected: FAIL. `Week`, `MealSlot`이 없다는 컴파일 오류

- [ ] **Step 3:** `kotlinx-datetime`를 추가하고 `MealSlot.kt`와 `Week.kt`를 구현한다. `containing`은 `date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)`로 계산하고, `current`는 `clock.todayIn(timeZone)`을 쓴다.
- [ ] **Step 4: 통과 확인.** 같은 명령을 실행한다. Expected: PASS. 10개 테스트 모두 통과
- [ ] **Step 5: Commit**

```powershell
git add -A gradle/libs.versions.toml shared
git commit -m "주 계산과 끼니 종류 추가"
```

---

### Task 4: 끼니 텍스트 검증과 한 주 식단 (TDD)

**설명할 개념:** `sealed interface`로 "성공/실패 종류"를 표현하는 방식, 바꿀 수 없는(immutable) 데이터 클래스를 복사해서 수정하는 방식

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/domain/MealText.kt`
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/domain/WeeklyMeals.kt`
- Create: `shared/src/commonTest/kotlin/com/mg/bubumeal/domain/MealTextTest.kt`
- Create: `shared/src/commonTest/kotlin/com/mg/bubumeal/domain/WeeklyMealsTest.kt`

**Interfaces:**
- Consumes: `Week`, `MealSlot` (Task 3)
- Produces:
  - `const val MAX_MEAL_TEXT_LENGTH = 50`
  - `sealed interface MealTextValidation { data class Valid(val text: String); data object Empty; data class TooLong(val length: Int) }` (세 타입 모두 `MealTextValidation`을 구현)
  - `fun validateMealText(input: String): MealTextValidation`: 앞뒤 공백을 지운 뒤 판단한다. 글자 수는 `String.length`(UTF-16 단위)로 센다. 서버(Kotlin)의 `length`와 같은 기준이다. 가운데 줄바꿈은 유지한다.
  - `data class MealCell(val date: LocalDate, val slot: MealSlot)`
  - `data class WeeklyMeals(val week: Week, val menus: Map<MealCell, String> = emptyMap())`
    - `val cells: List<MealCell>`: 21개. 날짜 순서로 놓고, 같은 날 안에서는 `MealSlot` 순서
    - `fun menuOf(cell: MealCell): String?`
    - `fun withMenu(cell: MealCell, text: String?): WeeklyMeals`: `null`이면 그 칸을 지운다. `cell.date !in week`이면 `IllegalArgumentException`. 텍스트 검증은 하지 않는다 (호출하는 쪽에서 `validateMealText`를 먼저 부른다)
    - `fun isEmpty(): Boolean`

- [ ] **Step 1: 실패하는 테스트 작성**

`MealTextTest`:

```kotlin
@Test fun exactly50_isValid() = assertEquals(MealTextValidation.Valid("가".repeat(50)), validateMealText("가".repeat(50)))
@Test fun length51_isTooLong() = assertEquals(MealTextValidation.TooLong(51), validateMealText("a".repeat(51)))
@Test fun trimsBeforeCheck() = assertEquals(MealTextValidation.Valid("김치찌개"), validateMealText("  김치찌개  "))
@Test fun blank_isEmpty() {
    assertEquals(MealTextValidation.Empty, validateMealText(""))
    assertEquals(MealTextValidation.Empty, validateMealText("  \n "))
}
@Test fun innerNewline_isKept() = assertEquals(MealTextValidation.Valid("밥\n국"), validateMealText("밥\n국"))
@Test fun emoji_countsByStringLength() {
    assertIs<MealTextValidation.Valid>(validateMealText("🍚".repeat(25)))
    assertEquals(MealTextValidation.TooLong(52), validateMealText("🍚".repeat(26)))
}
@Test fun trailingSpacesDoNotCountTowardLimit() =
    assertIs<MealTextValidation.Valid>(validateMealText("a".repeat(50) + "   "))
```

`WeeklyMealsTest` (`week = Week(LocalDate(2026, 11, 16))`):

```kotlin
@Test fun empty_has21EmptyCells() {
    val meals = WeeklyMeals(week)
    assertEquals(21, meals.cells.size)
    assertTrue(meals.cells.all { meals.menuOf(it) == null })
    assertTrue(meals.isEmpty())
}
@Test fun cells_orderedByDayThenSlot() {
    val cells = WeeklyMeals(week).cells
    assertEquals(MealCell(LocalDate(2026, 11, 16), MealSlot.BREAKFAST), cells.first())
    assertEquals(MealCell(LocalDate(2026, 11, 16), MealSlot.LUNCH), cells[1])
    assertEquals(MealCell(LocalDate(2026, 11, 22), MealSlot.DINNER), cells.last())
}
@Test fun withMenu_setsAndKeepsOriginal() {
    val cell = MealCell(LocalDate(2026, 11, 18), MealSlot.LUNCH)
    val original = WeeklyMeals(week)
    val updated = original.withMenu(cell, "비빔밥")
    assertEquals("비빔밥", updated.menuOf(cell))
    assertNull(original.menuOf(cell))
    assertFalse(updated.isEmpty())
}
@Test fun withMenu_nullRemoves() {
    val cell = MealCell(LocalDate(2026, 11, 18), MealSlot.LUNCH)
    assertNull(WeeklyMeals(week).withMenu(cell, "비빔밥").withMenu(cell, null).menuOf(cell))
}
@Test fun withMenu_outsideWeek_throws() {
    assertFailsWith<IllegalArgumentException> {
        WeeklyMeals(week).withMenu(MealCell(LocalDate(2026, 11, 23), MealSlot.BREAKFAST), "x")
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.domain.MealTextTest" --tests "com.mg.bubumeal.domain.WeeklyMealsTest"`
Expected: FAIL. 컴파일 오류

- [ ] **Step 3:** `MealText.kt`와 `WeeklyMeals.kt`를 위 Interfaces대로 구현한다.
- [ ] **Step 4: 통과 확인.** `.\gradlew.bat :shared:testAndroidHostTest`를 실행한다. Expected: PASS. Task 3 테스트도 모두 통과
- [ ] **Step 5: Commit**

```powershell
git add -A shared
git commit -m "끼니 텍스트 검증과 한 주 식단 모델 추가"
```

---

## 이 계획이 끝난 뒤

- `CLAUDE.md` 아키텍처 절에 새 패키지(`navigation`, `ui`, `domain`)를 한 줄씩 추가한다.
- 3단계(가짜 저장소와 주간 식단표 화면) 계획을 쓴다. 3단계는 이 계획의 `Week`, `WeeklyMeals`, `validateMealText`, `WeeklyRoute`를 쓴다.
- 서버도 글자 수를 `String.length`로 세는지 API 명세가 나오면 확인한다.
