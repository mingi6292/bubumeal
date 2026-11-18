package com.mg.bubumeal.data

import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import com.mg.bubumeal.domain.ProviderToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

// androidMain의 actual을 JVM에서 돌린다. runTest 안에서는 delay가 가상 시간으로 바로 지나간다.
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSocialLoginTest {
    @Test
    fun login_returnsFakeTokenForEachProvider() = runTest {
        val login = platformSocialLogin()
        LoginProvider.entries.forEach { provider ->
            assertEquals(
                LoginResult.Success(ProviderToken(provider, "fake-android-${provider.name.lowercase()}-token")),
                login.login(provider),
            )
        }
    }

    @Test
    fun login_waitsBeforeAnswering() = runTest {
        platformSocialLogin().login(LoginProvider.KAKAO)
        assertEquals(FAKE_LOGIN_DELAY_MS, testScheduler.currentTime)
    }
}
