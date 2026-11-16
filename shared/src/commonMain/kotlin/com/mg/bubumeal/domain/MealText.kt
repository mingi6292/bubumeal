package com.mg.bubumeal.domain

/** 끼니 텍스트 최대 글자 수. 이 값은 여기에서만 정의한다. */
const val MAX_MEAL_TEXT_LENGTH = 50

/** 끼니 텍스트 검증 결과. */
sealed interface MealTextValidation {
    /** 저장할 수 있다. [text]는 앞뒤 공백을 지운 값이다. */
    data class Valid(val text: String) : MealTextValidation

    /** 공백만 있거나 비어 있다. */
    data object Empty : MealTextValidation

    /** 너무 길다. [length]는 앞뒤 공백을 지운 뒤의 글자 수다. */
    data class TooLong(val length: Int) : MealTextValidation
}

/**
 * 앞뒤 공백을 지운 뒤 끼니 텍스트를 검증한다. 가운데 줄바꿈은 그대로 둔다.
 * 글자 수는 유니코드 문자(코드 포인트) 단위로 센다. `🍚`처럼 UTF-16 두 칸을 쓰는 문자도 한 글자다.
 */
fun validateMealText(input: String): MealTextValidation {
    val text = input.trim()
    if (text.isEmpty()) return MealTextValidation.Empty
    val length = text.length - text.count { it.isLowSurrogate() }
    return if (length > MAX_MEAL_TEXT_LENGTH) {
        MealTextValidation.TooLong(length)
    } else {
        MealTextValidation.Valid(text)
    }
}
