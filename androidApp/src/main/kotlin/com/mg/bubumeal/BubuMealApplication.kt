package com.mg.bubumeal

import android.app.Application
import com.mg.bubumeal.data.AndroidLoginHost

/**
 * 프로세스가 시작될 때 가장 먼저 만들어지는 객체(Application). Activity보다 먼저, 그리고 Activity가 없어도 실행된다.
 * 카카오 SDK는 로그인 도중 앱이 종료됐다 다시 열려도 동작해야 해서, 키 설정과 SDK 초기화를 Activity가 아니라 여기서 한다.
 */
class BubuMealApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidLoginHost.googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        AndroidLoginHost.kakaoNativeAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY
        AndroidLoginHost.initSdks(this)
    }
}
