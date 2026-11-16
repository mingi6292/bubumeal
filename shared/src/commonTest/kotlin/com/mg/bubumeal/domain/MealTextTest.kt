package com.mg.bubumeal.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MealTextTest {

    @Test
    fun exactly50_isValid() =
        assertEquals(MealTextValidation.Valid("가".repeat(50)), validateMealText("가".repeat(50)))

    @Test
    fun length51_isTooLong() =
        assertEquals(MealTextValidation.TooLong(51), validateMealText("a".repeat(51)))

    @Test
    fun trimsBeforeCheck() =
        assertEquals(MealTextValidation.Valid("김치찌개"), validateMealText("  김치찌개  "))

    @Test
    fun blank_isEmpty() {
        assertEquals(MealTextValidation.Empty, validateMealText(""))
        assertEquals(MealTextValidation.Empty, validateMealText("  \n "))
    }

    @Test
    fun innerNewline_isKept() =
        assertEquals(MealTextValidation.Valid("밥\n국"), validateMealText("밥\n국"))

    @Test
    fun emoji_countsByStringLength() {
        // 글자 수는 String.length(UTF-16 단위)로 센다. 🍚는 length가 2다.
        assertIs<MealTextValidation.Valid>(validateMealText("🍚".repeat(25)))
        assertEquals(MealTextValidation.TooLong(52), validateMealText("🍚".repeat(26)))
    }

    @Test
    fun trailingSpacesDoNotCountTowardLimit() {
        assertIs<MealTextValidation.Valid>(validateMealText("a".repeat(50) + "   "))
    }
}
