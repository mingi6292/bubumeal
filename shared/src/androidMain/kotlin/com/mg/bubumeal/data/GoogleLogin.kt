package com.mg.bubumeal.data

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import com.mg.bubumeal.domain.ProviderToken
import com.mg.bubumeal.domain.TokenKind
import kotlin.coroutines.cancellation.CancellationException

/** 구글 ID 토큰을 받아 오는 일. 실패하면 예외를 던진다. 테스트에서는 가짜로 바꾼다. */
internal fun interface GoogleIdTokenFetcher {
    suspend fun fetch(activity: Activity): String
}

/** Credential Manager로 "Google로 로그인" 창을 띄워 ID 토큰을 받는다. */
internal class CredentialManagerFetcher(private val serverClientId: String) : GoogleIdTokenFetcher {
    override suspend fun fetch(activity: Activity): String {
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "구글 ID 토큰이 아닌 자격 증명이 왔다"
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}

/** 구글 로그인. 구글은 ID 토큰을 준다. 서버가 어느 종류를 받을지는 아직 미정이다. */
internal class GoogleSocialLogin(
    private val fetcher: GoogleIdTokenFetcher,
    private val activityProvider: () -> Activity?,
) : SocialLogin {
    override suspend fun login(provider: LoginProvider): LoginResult {
        require(provider == LoginProvider.GOOGLE) { "구글만 처리한다: $provider" }
        // 화면이 없으면(백그라운드, 종료 중) 창을 띄울 수 없다.
        val activity = activityProvider() ?: return LoginResult.Failed
        return try {
            val idToken = fetcher.fetch(activity)
            if (idToken.isBlank()) LoginResult.Failed
            else LoginResult.Success(ProviderToken(LoginProvider.GOOGLE, TokenKind.ID, idToken))
        } catch (e: CancellationException) {
            throw e // 코루틴 취소는 로그인 취소가 아니다. 그대로 위로 보낸다.
        } catch (e: Exception) {
            googleErrorToResult(e)
        }
    }
}

/**
 * 구글 로그인 예외를 화면에 보일 결과로 바꾼다.
 * 사용자가 창을 닫은 것만 [LoginResult.Cancelled]이고, 토큰을 못 받음, 제공자 오류, 네트워크 오류는 모두 [LoginResult.Failed]이다.
 */
internal fun googleErrorToResult(e: Exception): LoginResult.Failure =
    if (e is GetCredentialCancellationException) LoginResult.Cancelled else LoginResult.Failed
