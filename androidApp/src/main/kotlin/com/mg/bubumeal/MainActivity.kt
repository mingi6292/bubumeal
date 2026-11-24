package com.mg.bubumeal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mg.bubumeal.data.AndroidLoginHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 로그인 창을 띄울 Activity를 shared에 알린다. (로그인 키는 BubuMealApplication이 알린다.)
        AndroidLoginHost.attach(this)

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        AndroidLoginHost.detach(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
