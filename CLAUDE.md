# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 작업 규칙

- 기능 요구사항과 규칙은 `docs/app-developer-brief.md`를 따른다. 9장 협의 필요 항목은 임의로 정하지 말고 먼저 물어본다.
- 나는 KMP가 처음이다. 새 개념이 나오면 짧게 설명하고 진행한다.
- 공통 로직은 `shared/commonMain`에 두고, 플랫폼별 코드는 `expect`/`actual`로 나눈다.
- Windows 환경이라 iOS 빌드는 할 수 없다. 테스트는 `commonTest`와 `androidHostTest`로 확인한다.
- 터미널 명령은 PowerShell 기준으로 안내한다.
- 워크트리 세션에서는 에뮬레이터를 직접 켜지 않는다. 에뮬레이터는 사용자가 Android Studio에서 켜 둔 것을 쓴다. 워크트리 안에서 켜면 그 폴더가 잠겨 작업이 끝난 뒤 지울 수 없다.
- 세션은 `/background` 대신 `/exit`로 끝낸다. 백그라운드로 보낸 세션은 워크트리 폴더를 계속 잡고 있다.

## 프로젝트

부부밀(BubuMeal)은 두 사람(소유자 1명 + 동반자 최대 1명)이 함께 쓰는 주간 식단표 서비스의 **모바일 클라이언트**(Android + iOS)입니다. 백엔드(Kotlin + Spring Boot, 별도 저장소)는 아직 구현 중이라 엔드포인트와 응답 스키마가 **확정되지 않았습니다**.

클라이언트 코드 구조에 영향을 주는 핵심 도메인 규칙:
- 로그인은 간편 로그인만 지원합니다(카카오, 네이버, 구글, 애플). 앱이 제공자 토큰을 받아 서버로 보내면, 서버가 서비스 자체의 access/refresh 토큰을 발급합니다. 이메일/비밀번호 방식은 없습니다. Android에는 애플 SDK가 없어 애플 로그인은 웹 로그인 방식으로 처리해야 합니다.
- 한 사용자가 소유하는 식단표는 최대 하나지만, 다른 사람의 식단표에 동반자로도 참여할 수 있습니다. 따라서 앱은 접근 가능한 식단표(소유 + 참여) 목록을 보여주고 그 사이를 전환할 수 있어야 합니다.
- 공유 코드 발급과 식단표 삭제는 소유자만 할 수 있습니다. 그 외 편집 권한은 소유자와 동반자가 완전히 같습니다.
- 주간 식단표는 7일 × 아침/점심/저녁 구조이고, 끼니마다 자유 텍스트로 메뉴를 적습니다. 데이터가 없는 주는 빈 상태로 조회됩니다.
- UI에서 처리해야 할 에러 상황은 기획 문서 5장에 있습니다(코드 만료/이미 사용됨, 이미 동반자가 있음, 본인 코드 입력, 10분 안에 5번 틀리면 15분 차단, 연결 해제나 삭제 후 접근 상실).
- API 명세는 서버의 OpenAPI 문서(`/v3/api-docs`, Swagger UI `/swagger-ui.html`, 로컬 서버 `localhost:8080`)가 기준이 됩니다.

## 명령어 (PowerShell)

- Android 디버그 APK 빌드: `.\gradlew.bat :androidApp:assembleDebug`
- shared 테스트를 JVM(Android 호스트)에서 실행 (`commonTest` + `androidHostTest`): `.\gradlew.bat :shared:testAndroidHostTest`
- 테스트 클래스 하나만 실행: `.\gradlew.bat :shared:testAndroidHostTest --tests "com.mg.bubumeal.SharedCommonTest"`
- iOS 빌드와 테스트(`:shared:iosSimulatorArm64Test`, Xcode의 `iosApp/`)는 macOS에서만 되므로 이 환경에서는 실행하지 않는다.

린터/포매터는 아직 설정되어 있지 않습니다(`kotlin.code.style=official`만 있음). Gradle 구성 캐시와 빌드 캐시가 켜져 있습니다.

## 아키텍처

JetBrains KMP 템플릿으로 만든 Kotlin Multiplatform + Compose Multiplatform 프로젝트입니다. UI까지 공유하므로 두 플랫폼 모두 같은 Compose `App()`을 그립니다.

- `shared/`: 앱 코드와 UI 전부. 새 AGP 플러그인 `com.android.kotlin.multiplatform.library`를 쓰는 KMP 라이브러리입니다(별도 `android {}` 블록이 아니라 `kotlin { android { ... } }` 안에서 설정). 타깃은 Android, `iosArm64`, `iosSimulatorArm64`이고, iOS는 `Shared`라는 이름의 정적 프레임워크로 빌드됩니다.
  - `commonMain`: 공통 로직과 Compose UI. 진입점은 `App.kt`의 `App()`입니다. Compose 리소스는 `commonMain/composeResources`에 두고, 생성되는 `bubumeal.shared.generated.resources.Res`로 접근합니다.
    - `navigation/`: `@Serializable` 화면 경로(`Routes.kt`)와 `NavHost`(`AppNavHost.kt`). 화면 이동은 연타 방지를 위해 `navigateFrom`을 거치고, 식단표를 만들거나 참여하거나 잃은 뒤에는 `navigateClearingBackStack`(`popUpTo(graph.id)`)으로 백스택을 비웁니다. ViewModel이 상태에 남긴 이동(`exitTo`, `goToWeekly` 등)은 화면이 이동한 뒤 `exitHandled()`/`navigationHandled()`로 지워 한 번만 처리합니다. 화면 목록은 `docs/screen-flow.md`를 따릅니다.
    - `ui/`: 공통 화면 컴포저블. 화면마다 하위 패키지에 `XxxScreen`(상태를 받아 그리기만 함)과 `XxxViewModel`(`StateFlow`로 상태를 내보냄)을 둡니다. 지금은 `weekly/`(주간 식단표, 끼니 편집 창), `planswitch/`(식단표 전환, 소유한 식단표가 없을 때 "내 식단표 만들기"), `codeinput/`(공유 코드 입력, 에러 문구), `issuecode/`(공유 코드 발급), `login/`(간편 로그인 버튼과 로그인 후 이동), `start/`(식단표가 없을 때의 시작 화면, 만들기), `plansettings/`(역할별 버튼, 해제/나가기/삭제 확인 대화상자)가 있습니다. 접근 상실 안내 `AccessLostDialog`는 주간 식단표와 설정 화면이 함께 씁니다. 화면 문구와 날짜 표기는 `Labels.kt`에 있습니다.
    - `data/`: 저장소 인터페이스(`MealRepository`, `PlanRepository`, `ShareCodeRepository`)와 지금 보고 있는 식단표(`CurrentPlan`). 구현은 지금 메모리에만 두는 `Fake*`이고, 서버가 나오면 진짜 구현으로 바꿉니다. `FakePlanRepository`의 목록에서 빠진 식단표는 접근 상실이 되고(`FakeMealRepository`도 `isAccessible`로 따름), 디버그용 `loseAccess`로 상대가 지운 상황을 흉내 냅니다(설정 화면의 [디버그] 버튼). 삭제·나가기·접근 상실 뒤 갈 곳은 `PlanExit.kt`의 `moveToRemainingPlan`이 정합니다(남은 식단표 중 소유 먼저, 없으면 시작). `FakeShareCodeRepository`는 정해 둔 코드(`CODE_SUCCESS` 등)로 서버 에러를 흉내 냅니다. 간편 로그인은 인터페이스 `SocialLogin`과 `expect fun platformSocialLogin()`(`SocialLogin.kt`)으로 나눠, ViewModel은 인터페이스만 압니다.
    - `AppContainer.kt`: DI 도구 없이 저장소와 `CurrentPlan`을 손으로 연결합니다. 화면을 회전해도 데이터가 남도록 `App.kt`에서 프로세스에 하나만 만듭니다.
    - `domain/`: UI와 무관한 순수 Kotlin 도메인 타입(`Week`, `MealSlot`, `WeeklyMeals`, `validateMealText`, `PlanSummary`(동반자 유무 `hasCompanion` 포함), 식단표 결과 `CreatePlanResult`/`PlanActionResult`/`PlanAccess`, `validateShareCode`, `JoinResult`, `IssueResult`). 날짜는 `kotlinx-datetime`을 씁니다.
  - `androidMain` / `iosMain`: `expect` 선언의 `actual` 구현(예: `getPlatform()`, `data/SocialLogin.*.kt`의 간편 로그인). Android의 구글 로그인은 Credential Manager로 진짜 구현이고(`GoogleLogin.kt`), 웹 클라이언트 ID는 `local.properties`의 `GOOGLE_WEB_CLIENT_ID`에서 읽어 `BuildConfig`로 넘긴다. 값이 비면 가짜로 돌아간다. `MainActivity`가 `AndroidLoginHost`에 현재 Activity를 약한 참조로 등록한다. 카카오, 네이버, 애플과 iOS는 아직 가짜(실제 SDK로 바꿀 자리). 제공자 토큰은 `TokenKind`(ID/ACCESS)를 함께 담고, 서버가 받을 종류는 기획 9장 미정. `iosMain/MainViewController.kt`는 `App()`을 `ComposeUIViewController`로 감쌉니다.
  - 테스트: `commonTest`(모든 타깃에서 실행), `androidHostTest`(JVM), `iosTest`. `kotlin.test`를 씁니다.
- `androidApp/`: 얇은 Android 앱 껍데기. `MainActivity`는 `App()`만 호출합니다. 패키지/applicationId는 `com.mg.bubumeal`입니다.
- `iosApp/`: 얇은 SwiftUI 껍데기. `ContentView`가 `Shared` 프레임워크의 `MainViewControllerKt.MainViewController()`를 띄웁니다.

의존성과 버전은 `gradle/libs.versions.toml`에서 한곳에 관리합니다(Kotlin 2.4, AGP 9.1, Compose Multiplatform 1.12, compile/target SDK 37, min SDK 24, JVM 11). 템플릿의 Greeting/Platform 코드는 자리 표시용이라 교체해도 됩니다.
