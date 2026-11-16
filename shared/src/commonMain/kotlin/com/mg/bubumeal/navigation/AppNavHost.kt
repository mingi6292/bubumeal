package com.mg.bubumeal.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mg.bubumeal.ui.PlaceholderScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
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
            PlaceholderScreen(
                title = "공유 코드 입력",
                actions = listOf(
                    "가짜 성공" to {
                        // 어디서 왔든 백스택을 모두 비우고 주간 식단표 하나만 남긴다.
                        // 그래프 전체를 지우는 방식이다. 에뮬레이터에서 확인함. WeeklyRoute에 인자가 생기면 다시 확인한다.
                        navController.navigateFrom(entry, WeeklyRoute) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                ),
            )
        }
        composable<WeeklyRoute> { entry ->
            PlaceholderScreen(
                title = "주간 식단표",
                actions = listOf(
                    "식단표 전환" to { navController.navigateFrom(entry, PlanSwitchRoute) },
                    "설정" to { navController.navigateFrom(entry, PlanSettingsRoute) },
                ),
            )
        }
        composable<PlanSwitchRoute> { entry ->
            PlaceholderScreen(
                title = "식단표 전환",
                actions = listOf(
                    "공유 코드 입력" to { navController.navigateFrom(entry, CodeInputRoute) },
                ),
            )
        }
        composable<PlanSettingsRoute> { entry ->
            PlaceholderScreen(
                title = "식단표 설정",
                actions = listOf(
                    "공유 코드 발급" to { navController.navigateFrom(entry, IssueCodeRoute) },
                ),
            )
        }
        composable<IssueCodeRoute> {
            PlaceholderScreen(title = "공유 코드 발급", actions = emptyList())
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
