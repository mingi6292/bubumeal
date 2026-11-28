package com.mg.bubumeal

import androidx.compose.runtime.Composable
import com.mg.bubumeal.navigation.AppNavHost
import com.mg.bubumeal.ui.theme.BubuMealTheme

// 프로세스에 하나만 둔다. Compose의 remember에 두면 화면을 회전할 때 Activity가 다시 만들어지면서
// 가짜 저장소의 데이터가 모두 사라진다.
private val appContainer by lazy { AppContainer() }

@Composable
fun App() {
    BubuMealTheme {
        AppNavHost(appContainer)
    }
}
