package com.mg.bubumeal.domain

// 간편 로그인 (기획 4.1). 앱은 제공자 토큰까지만 받고, 서비스 토큰은 서버가 발급한다.
// 서버 에러 형식이 나오면 Failure에 "토큰 검증 실패" 같은 갈래를 더한다.

/** 지원하는 제공자. 선언 순서가 로그인 버튼 순서다. */
enum class LoginProvider { KAKAO, NAVER, GOOGLE, APPLE }

/** 제공자가 준 토큰(ID 토큰 또는 access 토큰). 서버로 보내 검증받을 값이다. */
data class ProviderToken(val provider: LoginProvider, val value: String)

/** 제공자 로그인 결과. */
sealed interface LoginResult {
    data class Success(val token: ProviderToken) : LoginResult

    /** 실패. 화면이 이유마다 다른 안내를 보여 준다. */
    sealed interface Failure : LoginResult

    /** 사용자가 로그인 창에서 취소했다. */
    data object Cancelled : Failure

    /** 제공자 서버 오류나 네트워크 오류. */
    data object Failed : Failure
}
