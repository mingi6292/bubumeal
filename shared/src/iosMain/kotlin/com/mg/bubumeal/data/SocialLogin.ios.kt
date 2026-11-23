package com.mg.bubumeal.data

import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import kotlinx.coroutines.delay

// Windows에서는 컴파일 확인을 못 한다. macOS에서 :shared:iosSimulatorArm64Test로 확인한다.
actual fun platformSocialLogin(): SocialLogin = FakeIosSocialLogin()

/** 잠깐 기다린 뒤 항상 성공하는 가짜. */
private class FakeIosSocialLogin : SocialLogin {
    override suspend fun login(provider: LoginProvider): LoginResult {
        delay(FAKE_LOGIN_DELAY_MS)
        when (provider) {
            LoginProvider.KAKAO -> Unit // 실제: 카카오 로그인 SDK
            LoginProvider.NAVER -> Unit // 실제: 네이버 아이디로 로그인 SDK
            LoginProvider.GOOGLE -> Unit // 실제: Google Sign-In SDK
            LoginProvider.APPLE -> Unit // 실제: 기본 제공 Sign in with Apple(AuthenticationServices)
        }
        return LoginResult.Success(fakeProviderToken("ios", provider))
    }
}
