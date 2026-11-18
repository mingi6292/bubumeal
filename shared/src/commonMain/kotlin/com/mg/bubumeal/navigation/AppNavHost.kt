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
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.LaunchedEffect
import com.mg.bubumeal.AppContainer
import com.mg.bubumeal.data.PlanExitDestination
import com.mg.bubumeal.ui.codeinput.CodeInputScreen
import com.mg.bubumeal.ui.codeinput.CodeInputViewModel
import com.mg.bubumeal.ui.issuecode.IssueCodeScreen
import com.mg.bubumeal.ui.issuecode.IssueCodeViewModel
import com.mg.bubumeal.ui.login.LoginDestination
import com.mg.bubumeal.ui.login.LoginScreen
import com.mg.bubumeal.ui.login.LoginViewModel
import com.mg.bubumeal.ui.planswitch.PlanSwitchScreen
import com.mg.bubumeal.ui.planswitch.PlanSwitchViewModel
import com.mg.bubumeal.ui.plansettings.PlanSettingsScreen
import com.mg.bubumeal.ui.plansettings.PlanSettingsViewModel
import com.mg.bubumeal.ui.start.StartScreen
import com.mg.bubumeal.ui.start.StartViewModel
import com.mg.bubumeal.ui.weekly.WeeklyScreen
import com.mg.bubumeal.ui.weekly.WeeklyViewModel

@Composable
fun AppNavHost(container: AppContainer, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = LoginRoute) {
        composable<LoginRoute> { entry ->
            val vm = viewModel { LoginViewModel(container.socialLogin, container.planRepository) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.destination) {
                val route: Any = when (state.destination) {
                    LoginDestination.WEEKLY -> WeeklyRoute
                    LoginDestination.START -> StartRoute
                    null -> return@LaunchedEffect
                }
                // 로그인 창(다른 Activity)이 앞에 떠 있는 동안 결과가 와도 이동이 버려지지 않게 돌아올 때까지 기다린다.
                entry.lifecycle.awaitResumed()
                // 로그인 화면을 백스택에서 지워, 뒤로 가기로 돌아오지 않게 한다.
                navController.navigateFrom(entry, route) {
                    popUpTo<LoginRoute> { inclusive = true }
                }
            }
            LoginScreen(state = state, onLogin = vm::login)
        }
        composable<StartRoute> { entry ->
            val vm = viewModel { StartViewModel(container.planRepository, container.currentPlan) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            // 만들기에 성공하면 ViewModel이 이미 CurrentPlan을 새 식단표로 바꿔 두었다.
            LaunchedEffect(state.goToWeekly) {
                if (!state.goToWeekly) return@LaunchedEffect
                entry.lifecycle.awaitResumed()
                navController.navigateClearingBackStack(entry, WeeklyRoute)
                vm.navigationHandled()
            }
            StartScreen(
                state = state,
                onCreate = vm::create,
                onEnterCode = { navController.navigateFrom(entry, CodeInputRoute) },
            )
        }
        composable<CodeInputRoute> { entry ->
            val vm = viewModel { CodeInputViewModel(container.shareCodeRepository, container.currentPlan) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            // 연결에 성공하면 ViewModel이 이미 CurrentPlan을 새 식단표로 바꿔 두었다.
            LaunchedEffect(state.joined) {
                if (!state.joined) return@LaunchedEffect
                entry.lifecycle.awaitResumed()
                // 어디서 왔든 백스택을 모두 비우고 주간 식단표 하나만 남긴다.
                // 그래프 전체를 지우는 방식이다. 에뮬레이터에서 확인함. WeeklyRoute에 인자가 생기면 다시 확인한다.
                navController.navigateClearingBackStack(entry, WeeklyRoute)
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
            // 접근 상실 뒤 남은 식단표가 없을 때만 시작 화면으로 간다. 남아 있으면 이 화면이 그 식단표를 다시 불러온다.
            LaunchedEffect(state.exitTo) {
                if (state.exitTo != PlanExitDestination.START) return@LaunchedEffect
                entry.lifecycle.awaitResumed()
                navController.navigateClearingBackStack(entry, StartRoute)
                vm.exitHandled()
            }
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
                onConfirmAccessLost = vm::confirmAccessLost,
            )
        }
        composable<PlanSwitchRoute> { entry ->
            val vm = viewModel { PlanSwitchViewModel(container.planRepository, container.currentPlan) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            // 주간 식단표까지만 돌아간다. 빠르게 두 번 눌러도 주간 식단표를 지나 앱이 닫히지 않는다.
            val backToWeekly = { navController.popBackStack<WeeklyRoute>(inclusive = false) }
            // 내 식단표를 만들면 ViewModel이 CurrentPlan을 새 식단표로 바꿔 두었으므로, 주간 식단표로 돌아가기만 하면 된다.
            LaunchedEffect(state.created) {
                if (!state.created) return@LaunchedEffect
                entry.lifecycle.awaitResumed()
                backToWeekly()
                vm.createdHandled()
            }
            PlanSwitchScreen(
                state = state,
                onSelect = { plan ->
                    vm.select(plan)
                    backToWeekly()
                },
                onEnterCode = { navController.navigateFrom(entry, CodeInputRoute) },
                onCreateOwn = vm::createOwn,
                onBack = { backToWeekly() },
            )
        }
        composable<PlanSettingsRoute> { entry ->
            val vm = viewModel { PlanSettingsViewModel(container.planRepository, container.currentPlan) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            // 삭제, 나가기, 접근 상실 뒤: 남은 식단표의 주간 식단표나 시작 화면으로 가고, 뒤로 가기로 돌아오지 않게 백스택을 비운다.
            LaunchedEffect(state.exitTo) {
                val route: Any = when (state.exitTo) {
                    PlanExitDestination.WEEKLY -> WeeklyRoute
                    PlanExitDestination.START -> StartRoute
                    null -> return@LaunchedEffect
                }
                entry.lifecycle.awaitResumed()
                navController.navigateClearingBackStack(entry, route)
                vm.exitHandled()
            }
            PlanSettingsScreen(
                state = state,
                onIssueCode = { navController.navigateFrom(entry, IssueCodeRoute) },
                onRemoveCompanion = vm::requestRemoveCompanion,
                onLeave = vm::requestLeave,
                onDelete = vm::requestDelete,
                onConfirmDialog = vm::confirmDialog,
                onDismissDialog = vm::dismissDialog,
                // 해제, 나가기, 삭제를 보내는 중에는 떠나지 않는다. 떠나면 끝난 뒤의 이동이 사라진다.
                onBack = { if (!state.isWorking) navController.popBackStack(entry.destination.id, inclusive = true) },
                onDebugLoseAccess = container.debugLoseAccess?.let { lose ->
                    { state.plan?.let { lose(it.id) } }
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
 * RESUMED가 될 때까지 기다린다. 상태(ViewModel 값)를 보고 이동할 때 [navigateFrom] 앞에 부른다.
 * 그러지 않으면 화면이 잠깐 가려진 순간에 온 결과로는 이동이 무시되고, 같은 값으로는 다시 시도되지 않는다.
 */
suspend fun Lifecycle.awaitResumed() {
    currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
}

/**
 * [from] 화면이 지금 활성 상태일 때만 [route]로 이동한다.
 * 버튼을 빠르게 두 번 누르면 두 번째 탭은 화면이 이미 바뀌는 중이라 무시되어, 같은 화면이 두 번 쌓이지 않는다.
 * 맨 위가 이미 같은 화면이면 새로 쌓지 않는다(launchSingleTop).
 */
/**
 * [route]로 가면서 백스택을 모두 비운다. 그래프 전체(graph.id)까지 지우므로 어디서 왔든 [route] 하나만 남고,
 * 뒤로 가기를 누르면 앱이 닫힌다. 식단표를 만들거나 참여한 뒤, 식단표를 잃은 뒤에 쓴다.
 */
private fun <T : Any> NavHostController.navigateClearingBackStack(from: NavBackStackEntry, route: T) =
    navigateFrom(from, route) {
        popUpTo(graph.id) { inclusive = true }
    }

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
