package com.mg.bubumeal.data

import android.app.Activity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.AuthError
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import com.mg.bubumeal.domain.ProviderToken
import com.mg.bubumeal.domain.TokenKind
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 카카오 access 토큰을 받아 오는 일. 실패하면 예외를 던진다. 테스트에서는 가짜로 바꾼다. */
internal fun interface KakaoAccessTokenFetcher {
    suspend fun fetch(activity: Activity): String
}

/**
 * 카카오 SDK로 로그인한다. 카카오톡 앱이 있으면 앱으로, 없거나 앱 로그인이 사용자 취소 외의 이유로 실패하면
 * 카카오계정 웹 로그인으로 넘어간다. (카카오 공식 안내와 같은 방식. 앱에서 사용자가 취소했다면 웹으로 다시 묻지 않는다.)
 */
internal class KakaoSdkFetcher : KakaoAccessTokenFetcher {
    override suspend fun fetch(activity: Activity): String {
        val client = UserApiClient.instance
        if (client.isKakaoTalkLoginAvailable(activity)) {
            try {
                return requestToken { callback -> client.loginWithKakaoTalk(activity, callback = callback) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!shouldFallbackToKakaoAccount(e)) throw e
            }
        }
        return requestToken { callback -> client.loginWithKakaoAccount(activity, callback = callback) }
    }

    /**
     * 카카오 SDK는 결과를 콜백으로 준다. 콜백을 `suspend` 함수로 바꿔 주는 어댑터다.
     * `suspendCancellableCoroutine`은 콜백이 불릴 때까지 코루틴을 멈춰 두었다가 `resume`으로 깨운다.
     */
    private suspend fun requestToken(start: ((OAuthToken?, Throwable?) -> Unit) -> Unit): String =
        suspendCancellableCoroutine { continuation ->
            start { token, error ->
                if (continuation.isActive) {
                    when {
                        error != null -> continuation.resumeWithException(error)
                        token != null -> continuation.resume(token.accessToken)
                        else -> continuation.resumeWithException(IllegalStateException("카카오가 토큰을 주지 않았다"))
                    }
                }
            }
        }
}

/** 카카오 로그인. 카카오는 access 토큰을 준다. 서버가 어느 종류를 받을지는 아직 미정이다. */
internal class KakaoSocialLogin(
    private val fetcher: KakaoAccessTokenFetcher,
    private val activityProvider: () -> Activity?,
) : SocialLogin {
    override suspend fun login(provider: LoginProvider): LoginResult {
        require(provider == LoginProvider.KAKAO) { "카카오만 처리한다: $provider" }
        // 화면이 없으면(백그라운드, 종료 중) 창을 띄울 수 없다.
        val activity = activityProvider() ?: return LoginResult.Failed
        return try {
            val accessToken = fetcher.fetch(activity)
            if (accessToken.isBlank()) LoginResult.Failed
            else LoginResult.Success(ProviderToken(LoginProvider.KAKAO, TokenKind.ACCESS, accessToken))
        } catch (e: CancellationException) {
            throw e // 코루틴 취소는 로그인 취소가 아니다. 그대로 위로 보낸다.
        } catch (e: Exception) {
            kakaoErrorToResult(e)
        }
    }
}

/** 사용자가 창을 닫았거나(SDK의 `Cancelled`), 동의 화면에서 거부한 경우(`access_denied`)다. */
internal fun isKakaoUserCancel(e: Throwable): Boolean =
    (e is ClientError && e.reason == ClientErrorCause.Cancelled) ||
        (e is AuthError && e.reason == AuthErrorCause.AccessDenied)

/** 카카오톡 앱 로그인이 사용자 취소가 아닌 이유(앱 미설치 상태 변화, 연동 오류 등)로 실패했을 때만 웹 로그인으로 넘어간다. */
internal fun shouldFallbackToKakaoAccount(e: Throwable): Boolean = !isKakaoUserCancel(e)

/**
 * 카카오 로그인 예외를 화면에 보일 결과로 바꾼다.
 * 사용자 취소만 [LoginResult.Cancelled]이고, 토큰을 못 받음, 제공자 오류, 네트워크 오류는 모두 [LoginResult.Failed]이다.
 */
internal fun kakaoErrorToResult(e: Throwable): LoginResult.Failure =
    if (isKakaoUserCancel(e)) LoginResult.Cancelled else LoginResult.Failed
