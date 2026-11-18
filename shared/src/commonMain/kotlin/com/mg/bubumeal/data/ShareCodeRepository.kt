package com.mg.bubumeal.data

import com.mg.bubumeal.domain.IssueResult
import com.mg.bubumeal.domain.JoinResult
import com.mg.bubumeal.domain.PlanId

// 서버가 나오면 공유 코드 API(BUB-13)를 부르는 구현으로 바꾼다.

interface ShareCodeRepository {
    /** [code]는 형식 검사를 통과한 값(대문자, 8자리)이다. 성공하면 접근 가능한 식단표 목록에도 들어간다. */
    suspend fun join(code: String): JoinResult

    /** 새 코드를 발급한다. 그 식단표의 이전 미사용 코드는 바로 무효가 된다. */
    suspend fun issue(planId: PlanId): IssueResult
}
