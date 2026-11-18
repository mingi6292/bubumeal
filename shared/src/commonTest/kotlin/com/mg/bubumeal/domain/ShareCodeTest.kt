package com.mg.bubumeal.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ShareCodeTest {

    @Test
    fun validCode_isValid() =
        assertEquals(ShareCodeValidation.Valid("ABCD2345"), validateShareCode("ABCD2345"))

    @Test
    fun lowercase_isUppercasedAndValid() =
        assertEquals(ShareCodeValidation.Valid("ABCD2345"), validateShareCode("abcd2345"))

    @Test
    fun surroundingSpaces_areTrimmedAndValid() =
        assertEquals(ShareCodeValidation.Valid("ABCD2345"), validateShareCode("  abcd2345 \n"))

    @Test
    fun blank_isEmpty() {
        assertEquals(ShareCodeValidation.Empty, validateShareCode(""))
        assertEquals(ShareCodeValidation.Empty, validateShareCode("   "))
    }

    @Test
    fun sevenCharacters_isWrongLength() =
        assertEquals(ShareCodeValidation.WrongLength(7), validateShareCode("ABCD234"))

    @Test
    fun nineCharacters_isWrongLength() =
        assertEquals(ShareCodeValidation.WrongLength(9), validateShareCode("ABCD23456"))

    @Test
    fun lengthIsCheckedBeforeCharacters() =
        assertEquals(ShareCodeValidation.WrongLength(3), validateShareCode("0O1"))

    @Test
    fun zero_isInvalidCharacter() =
        assertEquals(ShareCodeValidation.InvalidCharacter, validateShareCode("ABCD2340"))

    @Test
    fun letterO_isInvalidCharacter() =
        assertEquals(ShareCodeValidation.InvalidCharacter, validateShareCode("ABCD234O"))

    @Test
    fun one_isInvalidCharacter() =
        assertEquals(ShareCodeValidation.InvalidCharacter, validateShareCode("ABCD2341"))

    @Test
    fun letterI_isInvalidCharacter() =
        assertEquals(ShareCodeValidation.InvalidCharacter, validateShareCode("ABCD234I"))

    @Test
    fun lowercaseO_isUppercasedThenInvalid() =
        assertEquals(ShareCodeValidation.InvalidCharacter, validateShareCode("abcd234o"))

    @Test
    fun lowercaseI_isUppercasedThenInvalid() =
        assertEquals(ShareCodeValidation.InvalidCharacter, validateShareCode("abcd234i"))

    @Test
    fun specialCharacter_isInvalidCharacter() {
        assertEquals(ShareCodeValidation.InvalidCharacter, validateShareCode("ABCD-234"))
        assertEquals(ShareCodeValidation.InvalidCharacter, validateShareCode("ABCD 234"))
    }

    @Test
    fun korean_isInvalidCharacter() =
        assertEquals(ShareCodeValidation.InvalidCharacter, validateShareCode("ABCD234가"))
}
