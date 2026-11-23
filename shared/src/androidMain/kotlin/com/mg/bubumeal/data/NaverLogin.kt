package com.mg.bubumeal.data

import android.app.Activity
import com.mg.bubumeal.domain.LoginProvider
import com.mg.bubumeal.domain.LoginResult
import com.mg.bubumeal.domain.ProviderToken
import com.mg.bubumeal.domain.TokenKind
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.core.data.errorcode.NidOAuthErrorCode
import com.navercorp.nid.oauth.util.NidOAuthCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 네이버 SDK가 로그인 실패 때 주는 오류 코드와 설명. */
internal class NaverLoginException(val errorCode: String, val errorDescription: String) :
    Exception("네이버 로그인 실패: $errorCode ($errorDescription)")

/** 네이버 access 토큰을 받아 오는 일. 실패하면 예외를 던진다. 테스트에서는 가짜로 바꾼다. */
internal fun interface NaverAccessTokenFetcher {
    suspend fun fetch(activity: Activity): String
}

/**
 * 네이버 로그인 SDK로 로그인한다. 네이버 앱이 있으면 앱으로, 없으면 SDK가 알아서 웹(커스텀 탭)으로 연결한다.
 * SDK 초기화(클라이언트 ID/시크릿/앱 이름)는 [AndroidLoginHost.initSdks]에서 한다.
 */
internal class NaverSdkFetcher : NaverAccessTokenFetcher {
    /** 콜백 방식 SDK 호출을 `suspend` 함수로 바꾼다. 콜백이 불릴 때까지 코루틴을 멈춰 두었다가 `resume`으로 깨운다. */
    override suspend fun fetch(activity: Activity): String = suspendCancellableCoroutine { continuation ->
        NidOAuth.requestLogin(activity, object : NidOAuthCallback {
            override fun onSuccess() {
                if (!continuation.isActive) return
                val token = NidOAuth.getAccessToken()
                if (token.isNullOrBlank()) continuation.resumeWithException(IllegalStateException("네이버가 토큰을 주지 않았다"))
                else continuation.resume(token)
            }

            override fun onFailure(errorCode: String, errorDesc: String) {
                if (continuation.isActive) continuation.resumeWithException(NaverLoginException(errorCode, errorDesc))
            }
        })
    }
}

/** 네이버 로그인. 네이버는 access 토큰을 준다. 서버가 어느 종류를 받을지는 아직 미정이다. */
internal class NaverSocialLogin(
    private val fetcher: NaverAccessTokenFetcher,
    private val activityProvider: () -> Activity?,
) : SocialLogin {
    override suspend fun login(provider: LoginProvider): LoginResult {
        require(provider == LoginProvider.NAVER) { "네이버만 처리한다: $provider" }
        // 화면이 없으면(백그라운드, 종료 중) 창을 띄울 수 없다.
        val activity = activityProvider() ?: return LoginResult.Failed
        return try {
            val accessToken = fetcher.fetch(activity)
            if (accessToken.isBlank()) LoginResult.Failed
            else LoginResult.Success(ProviderToken(LoginProvider.NAVER, TokenKind.ACCESS, accessToken))
        } catch (e: CancellationException) {
            throw e // 코루틴 취소는 로그인 취소가 아니다. 그대로 위로 보낸다.
        } catch (e: Exception) {
            naverErrorToResult(e)
        }
    }
}

/** 사용자가 로그인 창을 닫았거나(`user_cancel`), 동의 화면에서 거부한 경우(`access_denied`)다. */
internal fun isNaverUserCancel(e: Throwable): Boolean =
    e is NaverLoginException &&
        (e.errorCode == NidOAuthErrorCode.CLIENT_USER_CANCEL.code || e.errorCode == NidOAuthErrorCode.SERVER_ERROR_ACCESS_DENIED.code)

/**
 * 네이버 로그인 예외를 화면에 보일 결과로 바꾼다.
 * 사용자 취소만 [LoginResult.Cancelled]이고, 토큰을 못 받음, 제공자 오류, 네트워크 오류는 모두 [LoginResult.Failed]이다.
 */
internal fun naverErrorToResult(e: Throwable): LoginResult.Failure =
    if (isNaverUserCancel(e)) LoginResult.Cancelled else LoginResult.Failed
