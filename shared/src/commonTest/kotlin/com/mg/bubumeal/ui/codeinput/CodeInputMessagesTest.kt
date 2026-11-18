package com.mg.bubumeal.ui.codeinput

import com.mg.bubumeal.domain.JoinResult
import com.mg.bubumeal.domain.ShareCodeValidation
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CodeInputMessagesTest {
    private val seoul = TimeZone.of("Asia/Seoul")

    private fun rejected(f: JoinResult.Failure) = CodeInputError.Rejected(f).message(seoul)

    @Test
    fun everyErrorHasDistinctMessage() {
        val messages = listOf(
            CodeInputError.Format(ShareCodeValidation.Empty),
            CodeInputError.Format(ShareCodeValidation.WrongLength(3)),
            CodeInputError.Format(ShareCodeValidation.InvalidCharacter),
            CodeInputError.Rejected(JoinResult.Expired),
            CodeInputError.Rejected(JoinResult.AlreadyUsed),
            CodeInputError.Rejected(JoinResult.PlanFull),
            CodeInputError.Rejected(JoinResult.OwnCode),
            CodeInputError.Rejected(JoinResult.WrongCode(2)),
            CodeInputError.Rejected(JoinResult.Blocked(Instant.parse("2026-11-18T06:05:00Z"))),
        ).map { it.message(seoul) }
        assertEquals(messages.size, messages.toSet().size)
        assertTrue(messages.none { it.isBlank() })
    }

    @Test
    fun wrongCode_showsRemainingAttempts() = assertTrue("2번" in rejected(JoinResult.WrongCode(2)))

    @Test
    fun blocked_showsUntilTimeInLocalZone() =
        assertTrue("15:05" in rejected(JoinResult.Blocked(Instant.parse("2026-11-18T06:05:00Z"))))

    @Test
    fun wrongLength_showsCurrentLength() =
        assertTrue("3자리" in CodeInputError.Format(ShareCodeValidation.WrongLength(3)).message(seoul))
}
