package com.mg.bubumeal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import com.mg.bubumeal.AppContainer
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.ui.PlaceholderScreen
import com.mg.bubumeal.ui.codeinput.CodeInputScreen
import com.mg.bubumeal.ui.codeinput.CodeInputViewModel
import com.mg.bubumeal.ui.issuecode.IssueCodeScreen
import com.mg.bubumeal.ui.issuecode.IssueCodeViewModel
import com.mg.bubumeal.ui.planswitch.PlanSwitchScreen
import com.mg.bubumeal.ui.planswitch.PlanSwitchViewModel
import com.mg.bubumeal.ui.weekly.WeeklyScreen
import com.mg.bubumeal.ui.weekly.WeeklyViewModel

@Composable
fun AppNavHost(container: AppContainer, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = LoginRoute) {
        composable<LoginRoute> { entry ->
            PlaceholderScreen(
                title = "로그인",
                actions = listOf(
                    "가짜 로그인" to {
                        // 로그인 화면을 백스택에서 지워, 뒤로 가기로 돌아오지 않게 한다.
                        navController.navigateFrom(entry, StartRoute) {
                            popUpTo<LoginRoute> { inclusive = true }
                        }
                    },
                ),
            )
        }
        composable<StartRoute> { entry ->
            PlaceholderScreen(
                title = "시작",
                actions = listOf(
                    "식단표 만들기" to {
                        navController.navigateFrom(entry, WeeklyRoute) {
                            popUpTo<StartRoute> { inclusive = true }
                        }
                    },
                    "공유 코드 입력" to { navController.navigateFrom(entry, CodeInputRoute) },
                ),
            )
        }
        composable<CodeInputRoute> { entry ->
            val vm = viewModel { CodeInputViewModel(container.shareCodeRepository, container.currentPlan) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            // 연결에 성공하면 ViewModel이 이미 CurrentPlan을 새 식단표로 바꿔 두었다.
            LaunchedEffect(state.joined) {
                if (!state.joined) return@LaunchedEffect
                // 어디서 왔든 백스택을 모두 비우고 주간 식단표 하나만 남긴다.
                // 그래프 전체를 지우는 방식이다. 에뮬레이터에서 확인함. WeeklyRoute에 인자가 생기면 다시 확인한다.
                navController.navigateFrom(entry, WeeklyRoute) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            CodeInputScreen(
                state = state,
                onSubmit = vm::submit,
                onTextChange = vm::clearError,
                onBack = { navController.popBackStack(entry.destination.id, inclusive = true) },
            )
        }
        composable<WeeklyRoute> { entry ->
            // 이 화면(백스택 항목)에 묶인 ViewModel. 화면을 회전해도 같은 객체가 유지된다.
            val vm = viewModel {
                WeeklyViewModel(container.mealRepository, container.planRepository, container.currentPlan)
            }
            val state by vm.uiState.collectAsStateWithLifecycle()
            WeeklyScreen(
                state = state,
                onPreviousWeek = vm::previousWeek,
                onNextWeek = vm::nextWeek,
                onCurrentWeek = vm::goToCurrentWeek,
                onCellClick = vm::openEditor,
                onSwitchPlan = { navController.navigateFrom(entry, PlanSwitchRoute) },
                onOpenSettings = { navController.navigateFrom(entry, PlanSettingsRoute) },
                onSave = vm::saveMeal,
                onDelete = vm::deleteMeal,
                onDismissEditor = vm::closeEditor,
            )
        }
        composable<PlanSwitchRoute> { entry ->
            val vm = viewModel { PlanSwitchViewModel(container.planRepository, container.currentPlan) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            // 주간 식단표까지만 돌아간다. 빠르게 두 번 눌러도 주간 식단표를 지나 앱이 닫히지 않는다.
            val backToWeekly = { navController.popBackStack<WeeklyRoute>(inclusive = false) }
            PlanSwitchScreen(
                state = state,
                onSelect = { plan ->
                    vm.select(plan)
                    backToWeekly()
                },
                onEnterCode = { navController.navigateFrom(entry, CodeInputRoute) },
                onBack = { backToWeekly() },
            )
        }
        composable<PlanSettingsRoute> { entry ->
            val plan by container.currentPlan.selected.collectAsStateWithLifecycle()
            PlaceholderScreen(
                title = "식단표 설정",
                // 발급은 소유자만 (docs/screen-flow.md 4장). 동반자가 있으면 숨기는 임시 결정은
                // 식단표에 동반자 정보가 생긴 뒤(설정 화면 단계)에 넣는다.
                actions = if (plan?.role == PlanRole.OWNER) {
                    listOf("공유 코드 발급" to { navController.navigateFrom(entry, IssueCodeRoute) })
                } else {
                    emptyList()
                },
            )
        }
        composable<IssueCodeRoute> { entry ->
            val vm = viewModel { IssueCodeViewModel(container.shareCodeRepository, container.currentPlan) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            IssueCodeScreen(
                state = state,
                onIssue = vm::issue,
                onBack = { navController.popBackStack(entry.destination.id, inclusive = true) },
            )
        }
    }
}

/**
 * [from] 화면이 지금 활성 상태일 때만 [route]로 이동한다.
 * 버튼을 빠르게 두 번 누르면 두 번째 탭은 화면이 이미 바뀌는 중이라 무시되어, 같은 화면이 두 번 쌓이지 않는다.
 * 맨 위가 이미 같은 화면이면 새로 쌓지 않는다(launchSingleTop).
 */
private fun <T : Any> NavHostController.navigateFrom(
    from: NavBackStackEntry,
    route: T,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    if (from.lifecycle.currentState != Lifecycle.State.RESUMED) return
    navigate(route) {
        launchSingleTop = true
        builder()
    }
}
