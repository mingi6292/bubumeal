package com.mg.bubumeal.data

import android.app.Activity
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
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

class GoogleSocialLoginTest {
    private val activity = Activity()

    private fun login(activityAvailable: Boolean = true, fetch: suspend () -> String) =
        GoogleSocialLogin({ fetch() }, { if (activityAvailable) activity else null })

    @Test
    fun success_carriesIdToken() = runTest {
        val result = login { "id-token" }.login(LoginProvider.GOOGLE)
        assertEquals(LoginResult.Success(ProviderToken(LoginProvider.GOOGLE, TokenKind.ID, "id-token")), result)
    }

    @Test
    fun userCancel_isCancelled() = runTest {
        assertEquals(LoginResult.Cancelled, login { throw GetCredentialCancellationException("취소") }.login(LoginProvider.GOOGLE))
    }

    @Test
    fun providerNetworkAndTokenErrors_areFailed() = runTest {
        val errors = listOf<Exception>(NoCredentialException("없음"), IOException("네트워크"), IllegalStateException("토큰 형식"))
        errors.forEach { error ->
            assertEquals(LoginResult.Failed, login { throw error }.login(LoginProvider.GOOGLE), error.toString())
        }
    }

    @Test
    fun blankToken_isFailed() = runTest {
        assertEquals(LoginResult.Failed, login { " " }.login(LoginProvider.GOOGLE))
    }

    @Test
    fun noActivity_isFailedWithoutFetching() = runTest {
        var fetched = false
        val result = login(activityAvailable = false) { fetched = true; "id-token" }.login(LoginProvider.GOOGLE)
        assertEquals(LoginResult.Failed, result)
        assertEquals(false, fetched)
    }

    @Test
    fun coroutineCancellation_isNotSwallowed() = runTest {
        assertFailsWith<CancellationException> { login { throw CancellationException("취소") }.login(LoginProvider.GOOGLE) }
    }
}
