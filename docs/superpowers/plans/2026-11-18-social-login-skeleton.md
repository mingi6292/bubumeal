# 간편 로그인 expect/actual 뼈대 (5단계) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 로그인 화면에서 카카오/네이버/구글/애플 버튼을 누르면 플랫폼별 가짜 로그인이 잠깐 기다린 뒤 제공자 토큰을 돌려주고, 성공하면 docs/screen-flow.md 5장대로 주간 식단표(식단표가 있을 때) 또는 시작 화면(없을 때)으로 이동하며, 취소/실패하면 안내를 보여 준다.

**Architecture:** `commonMain`에 제공자 종류와 결과 타입(`domain/Login.kt`), 로그인 인터페이스 `SocialLogin`과 이를 만드는 `expect fun platformSocialLogin()`(`data/SocialLogin.kt`)을 둔다. `androidMain`/`iosMain`의 `actual`은 가짜다. 화면은 3·4단계처럼 `ui/login/`에 `LoginScreen` + `LoginViewModel`을 두고, ViewModel은 `SocialLogin` 인터페이스와 `PlanRepository`만 안다(테스트에서는 대본대로 답하는 가짜를 끼운다). `AppContainer`가 `platformSocialLogin()`을 연결한다.

**Tech Stack:** Kotlin 2.4 Multiplatform, Compose Multiplatform 1.12 Material3, navigation-compose, lifecycle-viewmodel-compose, kotlinx-coroutines(+ `kotlinx-coroutines-test`), kotlin.test

**Spec:** `docs/plan.md` 5단계. 이동은 `docs/screen-flow.md` 2장·5장, 에러 상황은 `docs/app-developer-brief.md` 4.1장·5장.

## Global Constraints

- 제공자는 정확히 4개: 카카오, 네이버, 구글, 애플. 이메일/비밀번호 로그인은 없다.
- 로그인 결과는 세 갈래: 성공(제공자 토큰), 사용자가 취소, 실패.
- 실제 SDK 연동, 서버로 토큰 보내기, 토큰 저장은 하지 않는다. `actual`은 잠깐 기다린 뒤 가짜 토큰을 돌려준다.
- Android `actual`의 애플 로그인 자리에 "Android에는 애플 SDK가 없어 웹 로그인 방식으로 처리할 자리"라는 주석을 단다.
- 로그인 성공 후: 접근 가능한 식단표가 있으면 `WeeklyRoute`, 없으면 `StartRoute`. 로그인 화면은 백스택에서 지워 뒤로 가기로 돌아오지 않는다(screen-flow 5장).
- 화면 이동은 기존 `navigateFrom`을 거친다. 새 의존성은 넣지 않는다(Koin, Ktor, SDK 없음).
- 공통 로직은 `commonMain`, 플랫폼 코드는 `expect`/`actual`. 테스트는 `commonTest`와 `androidHostTest`. iOS 빌드는 하지 않는다(iOS `actual`은 macOS에서 따로 컴파일 확인).
- 명령은 PowerShell(`.\gradlew.bat`). 커밋 메시지는 한국어.
- 사용자는 KMP가 처음이다. Task 2 시작 전에 `expect`/`actual`과 플랫폼별 소스 세트를 짧게 설명하고, "부부밀 KMP 개념 노트"에도 추가한다.

## 이 계획에서 정한 것 (사용자 확인 필요)

| 항목 | 결정 | 이유 |
|---|---|---|
| `expect` 모양 | 공통 인터페이스 `SocialLogin` + `expect fun platformSocialLogin(): SocialLogin` | ViewModel 테스트에서 가짜를 끼울 수 있다. `expect class`는 테스트에서 바꿔 끼우기 어렵다 |
| 가짜 `actual`의 결과 | 항상 성공. 1초 기다린 뒤 `fake-<플랫폼>-<제공자>-token`을 돌려준다 | plan.md 5단계 그대로. 취소/실패 안내는 ViewModel 테스트로 확인한다 (에뮬레이터에서는 볼 수 없음) |
| 성공 후 제공자 토큰 | 지금은 버린다. "서버가 나오면 여기서 서버로 보낸다" 주석만 둔다 | 서버 교환과 토큰 저장은 범위 밖 |
| 안내 문구 | 취소: `로그인을 취소했어요.` / 실패: `로그인하지 못했어요. 잠시 후 다시 시도해 주세요.` | 기획에 문구 없음 |
| 버튼 문구 | `카카오로 로그인`, `네이버로 로그인`, `구글로 로그인`, `애플로 로그인` (이 순서) | 기획 문서의 나열 순서 |
| 로그인 중 | 네 버튼을 모두 끄고 진행 표시줄을 보인다. 그동안 누른 것은 무시한다 | 두 제공자 로그인이 겹치지 않게 |

**참고:** 가짜 식단표 저장소에는 식단표가 2개 들어 있어서, 에뮬레이터에서는 로그인 → 주간 식단표로 바로 간다(screen-flow 5장). plan.md의 "로그인 → 시작 화면" 경로는 식단표가 없을 때만 나오며, ViewModel 테스트로 확인한다.

## 이번에 하지 않는 것

- 서버의 "토큰 검증 실패" 에러: 서버 교환이 없어서 생기지 않는다. 서버 연동 때 `LoginResult.Failure`에 갈래를 더한다.
- 로그아웃, 자동 로그인(앱 재시작 시 로그인 건너뛰기): 토큰 저장이 없어 앱을 켤 때마다 로그인 화면부터 시작한다.
- 실제 SDK가 필요로 할 Android `Activity` 전달: SDK 연동 때 `platformSocialLogin()`의 인자로 정한다.

## Review Focus

1. **로그인 중 버튼 연타/다른 버튼 누르기** → 로그인은 한 번만 실행된다. Task 3 `login_whileInProgress_ignored`.
2. **로그인 성공 뒤 다시 누르기(이동 직전)** → 두 번째 로그인이 일어나지 않는다. Task 3 `login_afterSuccess_ignored`.
3. **취소 뒤 다시 시도** → 이전 안내가 사라지고 다시 로그인할 수 있다. Task 3 `login_retryAfterCancel_clearsError`.
4. **주간 식단표에서 뒤로 가기** → 로그인 화면으로 돌아가지 않고 앱이 닫힌다. Task 4 에뮬레이터 확인.
5. **로그인 중 화면 회전** → ViewModel이 살아 있어 진행 상태가 유지되고 이동도 한 번만 일어난다. Task 4 에뮬레이터 확인.

---

### Task 1: 로그인 도메인 타입과 문구

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/domain/Login.kt`
- Modify: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/Labels.kt`
- Test: `shared/src/commonTest/kotlin/com/mg/bubumeal/ui/LabelsTest.kt` (기존 파일에 추가)

**Interfaces:**
- Produces:
  - `enum class LoginProvider { KAKAO, NAVER, GOOGLE, APPLE }` (선언 순서 = 버튼 순서)
  - `data class ProviderToken(val provider: LoginProvider, val value: String)`
  - `sealed interface LoginResult { data class Success(val token: ProviderToken); sealed interface Failure : LoginResult; data object Cancelled : Failure; data object Failed : Failure }`
  - `fun LoginProvider.label(): String` → `카카오`, `네이버`, `구글`, `애플`
  - `fun LoginResult.Failure.message(): String` → Global의 안내 문구 (Labels.kt)

- [ ] **Step 1: 실패하는 테스트 작성** (`LabelsTest`에 추가)

```kotlin
@Test
fun loginProviderLabels_inButtonOrder() {
    assertEquals(listOf("카카오", "네이버", "구글", "애플"), LoginProvider.entries.map { it.label() })
}

@Test
fun loginFailureMessages() {
    assertEquals("로그인을 취소했어요.", LoginResult.Cancelled.message())
    assertEquals("로그인하지 못했어요. 잠시 후 다시 시도해 주세요.", LoginResult.Failed.message())
}
```

- [ ] **Step 2: 실패 확인** — `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.ui.LabelsTest"` → 컴파일 실패(`LoginProvider` 없음)
- [ ] **Step 3: `domain/Login.kt`와 Labels.kt 함수 구현.** `Login.kt` 위에 "서버 에러 형식이 나오면 `Failure`에 토큰 검증 실패 등을 더한다" 주석.
- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS
- [ ] **Step 5: 커밋** — `간편 로그인 제공자와 결과 타입 추가`

### Task 2: `SocialLogin` 인터페이스와 플랫폼별 가짜 `actual`

**설명할 개념 (작업 전):** `expect`/`actual`, 소스 세트(`commonMain`이 `androidMain`/`iosMain` 위에 있고 컴파일할 때 플랫폼별 `actual`이 붙는 구조), `androidHostTest`가 Android 쪽 `actual`을 JVM에서 돌린다는 점. 개념 노트에도 추가한다.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/data/SocialLogin.kt`
- Create: `shared/src/androidMain/kotlin/com/mg/bubumeal/data/SocialLogin.android.kt`
- Create: `shared/src/iosMain/kotlin/com/mg/bubumeal/data/SocialLogin.ios.kt`
- Modify: `shared/src/commonMain/kotlin/com/mg/bubumeal/AppContainer.kt` (`val socialLogin: SocialLogin = platformSocialLogin()`)
- Test: `shared/src/androidHostTest/kotlin/com/mg/bubumeal/data/AndroidSocialLoginTest.kt`

**Interfaces:**
- Consumes: Task 1의 `LoginProvider`, `LoginResult`, `ProviderToken`
- Produces:
  - `interface SocialLogin { suspend fun login(provider: LoginProvider): LoginResult }` (KDoc: 제공자 SDK/웹 로그인 창을 띄우고 제공자 토큰을 받는다. 서버 토큰 교환은 하지 않는다)
  - `expect fun platformSocialLogin(): SocialLogin`
  - `const val FAKE_LOGIN_DELAY_MS = 1_000L` (commonMain, 두 actual이 함께 씀)
  - Android: `private class FakeAndroidSocialLogin : SocialLogin`, 토큰 `fake-android-<provider 소문자>-token`
  - iOS: `private class FakeIosSocialLogin : SocialLogin`, 토큰 `fake-ios-<provider 소문자>-token`

- [ ] **Step 1: 실패하는 테스트 작성** (`androidHostTest`, `runTest`라 1초 대기는 가상 시간으로 지나간다)

```kotlin
class AndroidSocialLoginTest {
    @Test
    fun login_returnsFakeTokenForEachProvider() = runTest {
        val login = platformSocialLogin()
        LoginProvider.entries.forEach { provider ->
            val result = login.login(provider)
            assertEquals(
                LoginResult.Success(ProviderToken(provider, "fake-android-${provider.name.lowercase()}-token")),
                result,
            )
        }
    }

    @Test
    fun login_waitsBeforeAnswering() = runTest {
        platformSocialLogin().login(LoginProvider.KAKAO)
        assertEquals(FAKE_LOGIN_DELAY_MS, testScheduler.currentTime)
    }
}
```

- [ ] **Step 2: 실패 확인** — `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.data.AndroidSocialLoginTest"` → 컴파일 실패
- [ ] **Step 3: 구현.** 각 actual은 `delay(FAKE_LOGIN_DELAY_MS)` 뒤 Success. 두 actual 모두 `when (provider)`로 제공자마다 "실제 SDK로 바꿀 자리" 주석을 달고, Android의 `APPLE` 갈래에는 웹 로그인 주석을, iOS의 `APPLE` 갈래에는 "기본 제공 Sign in with Apple(AuthenticationServices)" 주석을 단다. Android 주석에 "실제 SDK는 Activity가 필요하므로 그때 인자를 정한다"도 적는다. AppContainer에 `socialLogin` 추가.
- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS. 이어서 `.\gradlew.bat :shared:testAndroidHostTest` 전체 PASS (AppContainer를 쓰는 기존 테스트 포함)
- [ ] **Step 5: 커밋** — `간편 로그인 expect/actual과 가짜 구현 추가`

### Task 3: `LoginViewModel`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/login/LoginViewModel.kt`
- Test: `shared/src/commonTest/kotlin/com/mg/bubumeal/ui/login/LoginViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 타입, Task 2 `SocialLogin`, 기존 `PlanRepository.accessiblePlans()`, `FakePlanRepository`
- Produces:
  - `enum class LoginDestination { WEEKLY, START }`
  - `data class LoginUiState(val inProgress: LoginProvider? = null, val error: LoginResult.Failure? = null, val destination: LoginDestination? = null)` — `destination`이 null이 아니면 화면이 이동한다
  - `class LoginViewModel(socialLogin: SocialLogin, planRepository: PlanRepository) : ViewModel()` + `val uiState: StateFlow<LoginUiState>` + `fun login(provider: LoginProvider)`

동작: `inProgress != null` 또는 `destination != null`이면 무시. 시작할 때 `error = null, inProgress = provider`. Success면 `accessiblePlans()`가 비었는지로 목적지를 정하고 `inProgress = null`. Failure면 `error = 결과, inProgress = null`. 식단표 선택은 하지 않는다(`WeeklyViewModel`이 이미 첫 식단표를 고른다).

- [ ] **Step 1: 실패하는 테스트 작성.** 테스트 파일 안에 대본 가짜를 둔다:

```kotlin
/** 다음에 돌려줄 결과를 정해 두는 가짜. [gate]를 주면 그것이 완료될 때까지 기다린다. */
private class ScriptedSocialLogin(var next: LoginResult, var gate: CompletableDeferred<Unit>? = null) : SocialLogin {
    val calls = mutableListOf<LoginProvider>()
    override suspend fun login(provider: LoginProvider): LoginResult {
        calls += provider
        gate?.await()
        return next
    }
}
```

`StandardTestDispatcher` + `Dispatchers.setMain`(기존 ViewModel 테스트와 같은 방식). 테스트와 단언:
- `login_success_withPlans_goesToWeekly`: 식단표 1개 → `destination == WEEKLY`, `inProgress == null`, `error == null`
- `login_success_noPlans_goesToStart`: 빈 목록 → `destination == START`
- `login_inProgress_showsProvider`: gate 걸고 `login(NAVER)` + `advanceUntilIdle()` → `inProgress == NAVER`
- `login_cancelled_showsError`: → `error == LoginResult.Cancelled`, `destination == null`, `inProgress == null`
- `login_failed_showsError`: → `error == LoginResult.Failed`
- `login_whileInProgress_ignored`: gate 걸고 `login(KAKAO)`, `login(GOOGLE)` → `calls == listOf(KAKAO)`
- `login_afterSuccess_ignored`: 성공 뒤 `login(APPLE)` → `calls.size == 1`
- `login_retryAfterCancel_clearsError`: 취소 뒤 `next = Success`, gate 걸고 다시 로그인 → 진행 중 `error == null`; gate 완료 뒤 `destination == WEEKLY`

- [ ] **Step 2: 실패 확인** — `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.ui.login.LoginViewModelTest"` → 컴파일 실패
- [ ] **Step 3: 구현.** 성공 갈래에 "서버가 나오면 여기서 제공자 토큰을 서버로 보내고 서비스 토큰을 받는다" 주석.
- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS
- [ ] **Step 5: 커밋** — `로그인 ViewModel 추가`

### Task 4: `LoginScreen`과 화면 이동

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mg/bubumeal/ui/login/LoginScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/mg/bubumeal/navigation/AppNavHost.kt` (`LoginRoute`의 `PlaceholderScreen` 교체)
- Modify: `CLAUDE.md` (아키텍처의 `ui/` 목록에 `login/`, `data/`에 `SocialLogin`, `androidMain`/`iosMain`에 로그인 actual 한 줄씩)

**Interfaces:**
- Consumes: Task 3 `LoginViewModel`, `LoginUiState`, `LoginDestination`; Task 1 `label()`, `message()`; Task 2 `AppContainer.socialLogin`
- Produces: `@Composable fun LoginScreen(state: LoginUiState, onLogin: (LoginProvider) -> Unit)`

- [ ] **Step 1: `LoginScreen` 구현.** 가운데 정렬 Column: 제목 `부부밀`, `LoginProvider.entries` 순서로 `"${label()}로 로그인"` 버튼(가로 꽉 채움, `inProgress != null`이면 모두 끔), 진행 중이면 `LinearProgressIndicator`, `error`가 있으면 `colorScheme.error` 색으로 `message()`. 제공자 브랜드 색과 로고는 넣지 않는다(SDK 연동 때 각 제공자 가이드를 따른다).
- [ ] **Step 2: `AppNavHost` 연결.** `viewModel { LoginViewModel(container.socialLogin, container.planRepository) }`, `LaunchedEffect(state.destination)`에서 `WEEKLY → WeeklyRoute`, `START → StartRoute`로 `navigateFrom(entry, …) { popUpTo<LoginRoute> { inclusive = true } }`. `CodeInputRoute`의 `joined` 처리와 같은 모양.
- [ ] **Step 3: 빌드와 전체 테스트** — `.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug` → BUILD SUCCESSFUL
- [ ] **Step 4: 에뮬레이터 확인** (사용자가 켜 둔 에뮬레이터에 `.\gradlew.bat :androidApp:installDebug`): 각 버튼 → 1초 진행 표시 → 주간 식단표. 진행 중 다른 버튼이 꺼져 있는지, 주간 식단표에서 뒤로 가기 하면 앱이 닫히는지, 로그인 중 회전해도 한 번만 이동하는지 확인. 에뮬레이터가 없으면 사용자에게 확인을 부탁하고 결과를 기다린다.
- [ ] **Step 5: CLAUDE.md 갱신 후 커밋** — `로그인 화면과 로그인 후 이동 추가`
- [ ] **Step 6: macOS 확인 항목 남기기** — 최종 보고에 "iOS `actual`(`SocialLogin.ios.kt`) 컴파일은 macOS에서 `:shared:iosSimulatorArm64Test`로 확인 필요"를 적는다.
