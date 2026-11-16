package com.mg.bubumeal

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.mg.bubumeal.navigation.AppNavHost

@Composable
fun App() {
    MaterialTheme {
        AppNavHost()
    }
}
