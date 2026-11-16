package com.mg.bubumeal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mg.bubumeal.ui.PlaceholderScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = LoginRoute) {
        composable<LoginRoute> {
            PlaceholderScreen(
                title = "로그인",
                actions = listOf(
                    "가짜 로그인" to {
                        // 로그인 화면을 백스택에서 지워, 뒤로 가기로 돌아오지 않게 한다.
                        navController.navigate(StartRoute) {
                            popUpTo<LoginRoute> { inclusive = true }
                        }
                    },
                ),
            )
        }
        composable<StartRoute> {
            PlaceholderScreen(
                title = "시작",
                actions = listOf(
                    "식단표 만들기" to {
                        navController.navigate(WeeklyRoute) {
                            popUpTo<StartRoute> { inclusive = true }
                        }
                    },
                    "공유 코드 입력" to { navController.navigate(CodeInputRoute) },
                ),
            )
        }
        composable<CodeInputRoute> {
            PlaceholderScreen(
                title = "공유 코드 입력",
                actions = listOf(
                    "가짜 성공" to {
                        // 어디서 왔든 백스택을 모두 비우고 주간 식단표 하나만 남긴다.
                        navController.navigate(WeeklyRoute) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                ),
            )
        }
        composable<WeeklyRoute> {
            PlaceholderScreen(
                title = "주간 식단표",
                actions = listOf(
                    "식단표 전환" to { navController.navigate(PlanSwitchRoute) },
                    "설정" to { navController.navigate(PlanSettingsRoute) },
                ),
            )
        }
        composable<PlanSwitchRoute> {
            PlaceholderScreen(
                title = "식단표 전환",
                actions = listOf(
                    "공유 코드 입력" to { navController.navigate(CodeInputRoute) },
                ),
            )
        }
        composable<PlanSettingsRoute> {
            PlaceholderScreen(
                title = "식단표 설정",
                actions = listOf(
                    "공유 코드 발급" to { navController.navigate(IssueCodeRoute) },
                ),
            )
        }
        composable<IssueCodeRoute> {
            PlaceholderScreen(title = "공유 코드 발급", actions = emptyList())
        }
    }
}
