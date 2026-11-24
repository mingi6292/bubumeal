package com.mg.bubumeal.data

import android.app.Activity
import com.kakao.sdk.common.model.AuthError
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.common.model.AuthErrorResponse
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import com.mg.bubumeal.domain.ProviderToken
import com.mg.bubumeal.domain.TokenKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KakaoSocialLoginTest {
    private val activity = Activity()

    private fun login(activityAvailable: Boolean = true, fetch: suspend () -> String) =
        KakaoSocialLogin({ fetch() }, { if (activityAvailable) activity else null })

    private val userCancel = ClientError(ClientErrorCause.Cancelled, "사용자 취소")

    @Test
    fun success_carriesAccessToken() = runTest {
        val result = login { "access-token" }.login(LoginProvider.KAKAO)
        assertEquals(LoginResult.Success(ProviderToken(LoginProvider.KAKAO, TokenKind.ACCESS, "access-token")), result)
    }

    @Test
    fun userCancel_isCancelled() = runTest {
        assertEquals(LoginResult.Cancelled, login { throw userCancel }.login(LoginProvider.KAKAO))
    }

    @Test
    fun consentDenied_isCancelled() = runTest {
        val denied = AuthError(400, AuthErrorCause.AccessDenied, AuthErrorResponse("access_denied", "거부"))
        assertEquals(LoginResult.Cancelled, login { throw denied }.login(LoginProvider.KAKAO))
    }

    @Test
    fun providerNetworkAndTokenErrors_areFailed() = runTest {
        val errors = listOf<Exception>(
            ClientError(ClientErrorCause.TokenNotFound, "토큰 없음"),
            AuthError(500, AuthErrorCause.ServerError, AuthErrorResponse("server_error", "서버 오류")),
            IOException("네트워크"),
            IllegalStateException("토큰 형식"),
        )
        errors.forEach { error ->
            assertEquals(LoginResult.Failed, login { throw error }.login(LoginProvider.KAKAO), error.toString())
        }
    }

    @Test
    fun blankToken_isFailed() = runTest {
        assertEquals(LoginResult.Failed, login { " " }.login(LoginProvider.KAKAO))
    }

    @Test
    fun noActivity_isFailedWithoutFetching() = runTest {
        var fetched = false
        val result = login(activityAvailable = false) { fetched = true; "access-token" }.login(LoginProvider.KAKAO)
        assertEquals(LoginResult.Failed, result)
        assertFalse(fetched)
    }

    @Test
    fun coroutineCancellation_isNotSwallowed() = runTest {
        assertFailsWith<CancellationException> { login { throw CancellationException("취소") }.login(LoginProvider.KAKAO) }
    }

    @Test
    fun talkLoginFallsBackToAccountExceptWhenUserCancelled() {
        assertFalse(shouldFallbackToKakaoAccount(userCancel))
        assertTrue(shouldFallbackToKakaoAccount(ClientError(ClientErrorCause.NotSupported, "카카오톡 미지원")))
        assertTrue(shouldFallbackToKakaoAccount(IOException("네트워크")))
    }
}
