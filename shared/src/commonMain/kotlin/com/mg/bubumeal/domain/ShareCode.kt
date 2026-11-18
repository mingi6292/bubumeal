package com.mg.bubumeal.domain

/** 공유 코드 자릿수. */
const val SHARE_CODE_LENGTH = 8

/** 공유 코드 형식 검증 결과. 앱에서 먼저 하는 검사이고, 최종 판단은 서버가 한다. */
sealed interface ShareCodeValidation {
    /** 형식이 맞다. [code]는 앞뒤 공백을 지우고 대문자로 바꾼 값이다. */
    data class Valid(val code: String) : ShareCodeValidation

    /** 공백만 있거나 비어 있다. */
    data object Empty : ShareCodeValidation

    /** 8자리가 아니다. [length]는 정규화한 뒤의 글자 수다. */
    data class WrongLength(val length: Int) : ShareCodeValidation

    /** 허용하지 않는 문자(0, O, 1, I, 특수문자, 한글 등)가 있다. */
    data object InvalidCharacter : ShareCodeValidation
}

/** 공유 코드에 쓸 수 있는 문자. 영문 대문자와 숫자에서 헷갈리는 0, O, 1, I를 뺐다. */
private const val SHARE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/**
 * 앞뒤 공백을 지우고 대문자로 바꾼 뒤 공유 코드 형식을 검사한다.
 * 비어 있는지 → 길이 → 문자 순서로 보고, 처음 걸린 이유 하나만 돌려준다.
 */
fun validateShareCode(input: String): ShareCodeValidation {
    val code = input.trim().uppercase()
    if (code.isEmpty()) return ShareCodeValidation.Empty
    if (code.length != SHARE_CODE_LENGTH) return ShareCodeValidation.WrongLength(code.length)
    if (!code.all { it in SHARE_CODE_ALPHABET }) return ShareCodeValidation.InvalidCharacter
    return ShareCodeValidation.Valid(code)
}
