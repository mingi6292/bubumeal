package com.mg.bubumeal.data

import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidSocialLoginRoutingTest {
    private val real = constant(LoginResult.Cancelled)
    private val fake = constant(LoginResult.Failed)

    private fun constant(result: LoginResult) = object : SocialLogin {
        override suspend fun login(provider: LoginProvider) = result
    }

    @Test
    fun providerWithRealImplementation_usesIt() = runTest {
        val login = AndroidSocialLogin(mapOf(LoginProvider.GOOGLE to real, LoginProvider.KAKAO to real), fake)
        assertEquals(LoginResult.Cancelled, login.login(LoginProvider.GOOGLE))
        assertEquals(LoginResult.Cancelled, login.login(LoginProvider.KAKAO))
    }

    @Test
    fun providersWithoutRealImplementation_stayFake() = runTest {
        val login = AndroidSocialLogin(mapOf(LoginProvider.GOOGLE to real), fake)
        listOf(LoginProvider.KAKAO, LoginProvider.NAVER, LoginProvider.APPLE).forEach {
            assertEquals(LoginResult.Failed, login.login(it))
        }
    }

    @Test
    fun withoutKeys_everyProviderIsFake() = runTest {
        LoginProvider.entries.forEach { assertEquals(LoginResult.Failed, AndroidSocialLogin(emptyMap(), fake).login(it)) }
    }
}
