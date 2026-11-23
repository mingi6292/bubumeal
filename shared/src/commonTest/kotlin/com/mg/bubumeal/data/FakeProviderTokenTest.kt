package com.mg.bubumeal.data

import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.TokenKind
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeProviderTokenTest {
    @Test
    fun googleAndApple_areIdTokens_kakaoAndNaver_areAccessTokens() {
        val kinds = LoginProvider.entries.associateWith { fakeProviderToken("test", it).kind }
        assertEquals(
            mapOf(
                LoginProvider.KAKAO to TokenKind.ACCESS,
                LoginProvider.NAVER to TokenKind.ACCESS,
                LoginProvider.GOOGLE to TokenKind.ID,
                LoginProvider.APPLE to TokenKind.ID,
            ),
            kinds,
        )
    }

    @Test
    fun value_includesPlatformAndProvider() {
        assertEquals("fake-ios-google-token", fakeProviderToken("ios", LoginProvider.GOOGLE).value)
    }
}
