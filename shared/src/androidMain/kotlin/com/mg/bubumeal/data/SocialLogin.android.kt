package com.mg.bubumeal.data

import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import kotlinx.coroutines.delay

// 실제 SDK는 로그인 창을 띄울 Activity가 필요하다. SDK를 붙일 때 platformSocialLogin()의 인자로 정한다.
actual fun platformSocialLogin(): SocialLogin = FakeAndroidSocialLogin()

/** 잠깐 기다린 뒤 항상 성공하는 가짜. */
private class FakeAndroidSocialLogin : SocialLogin {
    override suspend fun login(provider: LoginProvider): LoginResult {
        delay(FAKE_LOGIN_DELAY_MS)
        when (provider) {
            LoginProvider.KAKAO -> Unit // 실제: 카카오 로그인 SDK
            LoginProvider.NAVER -> Unit // 실제: 네이버 아이디로 로그인 SDK
            LoginProvider.GOOGLE -> Unit // 실제: Credential Manager(구글 로그인)
            // Android에는 애플 SDK가 없어 웹 로그인 방식(브라우저/커스텀 탭으로 애플 로그인 페이지)으로 처리할 자리다.
            LoginProvider.APPLE -> Unit
        }
        return LoginResult.Success(fakeProviderToken("android", provider))
    }
}
