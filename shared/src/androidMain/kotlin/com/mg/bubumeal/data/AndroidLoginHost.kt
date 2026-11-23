package com.mg.bubumeal.data

import android.app.Activity
import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import java.lang.ref.WeakReference

/**
 * 로그인 창을 띄울 Activity와 로그인 설정을 `androidApp`에서 `shared`로 넘기는 통로.
 *
 * [platformSocialLogin]은 `expect`/`actual`이라 인자를 받을 수 없고, 로그인 구현을 담은 AppContainer는
 * 화면을 회전해도 살아남는 프로세스 단위 객체다. 그래서 Activity를 구현에 직접 넣지 않고,
 * 이 홀더에 약한 참조로 두었다가 로그인할 때마다 그때의 Activity를 꺼내 쓴다.
 */
object AndroidLoginHost {
    private var activityRef: WeakReference<Activity>? = null

    /** 구글 웹 클라이언트 ID. 비어 있으면 구글도 가짜 로그인을 쓴다. */
    @Volatile
    var googleWebClientId: String = ""

    /** 카카오 네이티브 앱 키. 비어 있으면 카카오도 가짜 로그인을 쓴다. */
    @Volatile
    var kakaoNativeAppKey: String = ""

    /** 키를 채운 뒤 `Application.onCreate`에서 한 번 부른다. 키가 있는 제공자의 SDK만 초기화한다. */
    fun initSdks(application: Application) {
        if (kakaoNativeAppKey.isNotBlank()) KakaoSdk.init(application, kakaoNativeAppKey)
    }

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    /** 새 Activity가 이미 등록됐다면(회전 직후 순서) 지우지 않도록 같은 Activity일 때만 지운다. */
    fun detach(activity: Activity) {
        if (activityRef?.get() === activity) activityRef = null
    }

    fun currentActivity(): Activity? = activityRef?.get()
}
