package com.mg.bubumeal.data

import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import com.mg.bubumeal.domain.ProviderToken
import com.mg.bubumeal.domain.TokenKind

/**
 * 제공자의 SDK(또는 웹 로그인) 창을 띄우고 제공자 토큰을 받는다.
 * 받은 토큰을 서버로 보내 서비스 토큰과 바꾸는 일은 여기서 하지 않는다.
 */
interface SocialLogin {
    suspend fun login(provider: LoginProvider): LoginResult
}

/**
 * 플랫폼마다 다른 로그인 구현을 돌려준다. 실제 내용은 androidMain, iosMain의 actual에 있다.
 * 지금은 둘 다 가짜이고, SDK를 붙일 때 actual만 바꾼다.
 */
expect fun platformSocialLogin(): SocialLogin

/** 가짜 actual이 로그인 창을 흉내 내며 기다리는 시간. */
const val FAKE_LOGIN_DELAY_MS = 1_000L

/**
 * 가짜 actual들이 돌려주는 토큰. 종류는 제공자 방식(구글, 애플은 OIDC라 ID, 카카오, 네이버는 access)을
 * 흉내 낸 것일 뿐, 서버가 받을 종류를 정한 것이 아니다.
 */
fun fakeProviderToken(platform: String, provider: LoginProvider): ProviderToken {
    val kind = when (provider) {
        LoginProvider.GOOGLE, LoginProvider.APPLE -> TokenKind.ID
        LoginProvider.KAKAO, LoginProvider.NAVER -> TokenKind.ACCESS
    }
    return ProviderToken(provider, kind, "fake-$platform-${provider.name.lowercase()}-token")
}
