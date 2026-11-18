package com.mg.bubumeal.ui.codeinput

import com.mg.bubumeal.domain.JoinResult
import com.mg.bubumeal.domain.SHARE_CODE_LENGTH
import com.mg.bubumeal.domain.ShareCodeValidation
import com.mg.bubumeal.ui.dateTimeLabel
import kotlinx.datetime.TimeZone

/** 에러마다 다른 안내 문구 (기획 5장). */
fun CodeInputError.message(timeZone: TimeZone): String = when (this) {
    is CodeInputError.Format -> when (val v = validation) {
        ShareCodeValidation.Empty -> "공유 코드를 입력해 주세요."
        is ShareCodeValidation.WrongLength -> "공유 코드는 ${SHARE_CODE_LENGTH}자리예요. (지금 ${v.length}자리)"
        ShareCodeValidation.InvalidCharacter -> "영문과 숫자만 쓸 수 있어요. 0, O, 1, I는 코드에 없어요."
        is ShareCodeValidation.Valid -> "" // Valid는 에러가 아니라 여기 오지 않는다.
    }
    is CodeInputError.Rejected -> when (val f = failure) {
        JoinResult.Expired -> "유효 시간(3시간)이 지난 코드예요. 상대에게 새 코드를 받아 주세요."
        JoinResult.AlreadyUsed -> "이미 사용된 코드예요. 상대에게 새 코드를 받아 주세요."
        JoinResult.PlanFull -> "이 식단표에는 이미 동반자가 있어요."
        JoinResult.OwnCode -> "내가 발급한 코드는 입력할 수 없어요. 상대에게 코드를 전달해 주세요."
        is JoinResult.WrongCode -> "코드가 맞지 않아요. ${f.remainingAttempts}번 더 틀리면 잠시 입력이 막혀요."
        is JoinResult.Blocked ->
            "코드를 여러 번 틀려서 입력이 막혔어요. ${dateTimeLabel(f.until, timeZone)} 이후에 다시 시도해 주세요."
    }
}
