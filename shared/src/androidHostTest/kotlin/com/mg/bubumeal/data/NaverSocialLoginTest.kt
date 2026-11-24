package com.mg.bubumeal.data

import android.app.Activity
import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import com.mg.bubumeal.domain.ProviderToken
import com.mg.bubumeal.domain.TokenKind
import com.navercorp.nid.core.data.errorcode.NidOAuthErrorCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class NaverSocialLoginTest {
    private val activity = Activity()

    private fun login(activityAvailable: Boolean = true, fetch: suspend () -> String) =
        NaverSocialLogin({ fetch() }, { if (activityAvailable) activity else null })

    private fun error(code: NidOAuthErrorCode) = NaverLoginException(code.code, code.description)

    @Test
    fun success_carriesAccessToken() = runTest {
        val result = login { "access-token" }.login(LoginProvider.NAVER)
        assertEquals(LoginResult.Success(ProviderToken(LoginProvider.NAVER, TokenKind.ACCESS, "access-token")), result)
    }

    @Test
    fun userCancelAndConsentDenied_areCancelled() = runTest {
        listOf(NidOAuthErrorCode.CLIENT_USER_CANCEL, NidOAuthErrorCode.SERVER_ERROR_ACCESS_DENIED).forEach { code ->
            assertEquals(LoginResult.Cancelled, login { throw error(code) }.login(LoginProvider.NAVER), code.name)
        }
    }

    @Test
    fun providerNetworkAndTokenErrors_areFailed() = runTest {
        val errors = listOf<Exception>(
            error(NidOAuthErrorCode.SERVER_ERROR_SERVER_ERROR),
            error(NidOAuthErrorCode.CLIENT_ERROR_CONNECTION_ERROR),
            error(NidOAuthErrorCode.SDK_IS_NOT_INITIALIZED),
            IOException("네트워크"),
            IllegalStateException("네이버가 토큰을 주지 않았다"),
        )
        errors.forEach { e ->
            assertEquals(LoginResult.Failed, login { throw e }.login(LoginProvider.NAVER), e.toString())
        }
    }

    @Test
    fun blankToken_isFailed() = runTest {
        assertEquals(LoginResult.Failed, login { " " }.login(LoginProvider.NAVER))
    }

    @Test
    fun noActivity_isFailedWithoutFetching() = runTest {
        var fetched = false
        val result = login(activityAvailable = false) { fetched = true; "access-token" }.login(LoginProvider.NAVER)
        assertEquals(LoginResult.Failed, result)
        assertFalse(fetched)
    }

    @Test
    fun coroutineCancellation_isNotSwallowed() = runTest {
        assertFailsWith<CancellationException> { login { throw CancellationException("취소") }.login(LoginProvider.NAVER) }
    }
}
