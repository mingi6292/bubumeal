package com.mg.bubumeal.data

import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import kotlinx.coroutines.delay

/**
 * 구글은 웹 클라이언트 ID가 있을 때만 진짜 Credential Manager 로그인을 쓰고,
 * 그 외(ID가 없거나 다른 제공자)는 가짜로 둔다. 카카오, 네이버, 애플은 SDK를 붙일 때 바꾼다.
 */
actual fun platformSocialLogin(): SocialLogin {
    val clientId = AndroidLoginHost.googleWebClientId
    val google = if (clientId.isBlank()) null else GoogleSocialLogin(CredentialManagerFetcher(clientId), AndroidLoginHost::currentActivity)
    return AndroidSocialLogin(google, FakeAndroidSocialLogin())
}

/** 제공자에 맞는 구현으로 나눠 준다. [google]이 null이면 구글도 가짜다. */
internal class AndroidSocialLogin(
    private val google: SocialLogin?,
    private val fake: SocialLogin,
) : SocialLogin {
    override suspend fun login(provider: LoginProvider): LoginResult =
        if (provider == LoginProvider.GOOGLE && google != null) google.login(provider) else fake.login(provider)
}

/** 잠깐 기다린 뒤 항상 성공하는 가짜. */
internal class FakeAndroidSocialLogin : SocialLogin {
    override suspend fun login(provider: LoginProvider): LoginResult {
        delay(FAKE_LOGIN_DELAY_MS)
        when (provider) {
            LoginProvider.KAKAO -> Unit // 실제: 카카오 로그인 SDK
            LoginProvider.NAVER -> Unit // 실제: 네이버 아이디로 로그인 SDK
            LoginProvider.GOOGLE -> Unit // 웹 클라이언트 ID가 없을 때만 여기로 온다
            // Android에는 애플 SDK가 없어 웹 로그인 방식(브라우저/커스텀 탭으로 애플 로그인 페이지)으로 처리할 자리다.
            LoginProvider.APPLE -> Unit
        }
        return LoginResult.Success(fakeProviderToken("android", provider))
    }
}
