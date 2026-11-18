package com.mg.bubumeal

import com.mg.bubumeal.data.CurrentPlan
import com.mg.bubumeal.data.FakeMealRepository
import com.mg.bubumeal.data.FakePlanRepository
import com.mg.bubumeal.data.FakeShareCodeRepository
import com.mg.bubumeal.data.ShareCodeRepository
import com.mg.bubumeal.data.SocialLogin
import com.mg.bubumeal.data.platformSocialLogin
import com.mg.bubumeal.data.MealRepository
import com.mg.bubumeal.data.PlanRepository
import com.mg.bubumeal.domain.MealCell
import com.mg.bubumeal.domain.MealSlot
import com.mg.bubumeal.domain.PlanId
import com.mg.bubumeal.domain.PlanRole
import com.mg.bubumeal.domain.PlanSummary
import com.mg.bubumeal.domain.Week
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * 앱 전체에서 함께 쓰는 객체를 손으로 연결하는 곳 (DI 도구 대신).
 * 서버가 나오면 여기서 Fake 저장소만 진짜 구현으로 바꾼다.
 */
class AppContainer(
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    private val myPlan = PlanSummary(PlanId("my-plan"), PlanRole.OWNER)
    private val joinedPlan = PlanSummary(PlanId("joined-plan"), PlanRole.COMPANION)

    private val fakePlanRepository = FakePlanRepository(listOf(myPlan, joinedPlan))
    val planRepository: PlanRepository = fakePlanRepository

    // 가짜끼리는 서로 알아야 한다. 코드로 연결에 성공하면 식단표 목록에 더해야 하기 때문이다.
    val shareCodeRepository: ShareCodeRepository = FakeShareCodeRepository(fakePlanRepository, clock)

    val mealRepository: MealRepository = run {
        // 전환해 보면 차이가 보이도록 참여 중인 식단표의 이번 주에만 메뉴를 미리 넣는다.
        val days = Week.current(clock, timeZone).days
        FakeMealRepository(
            mapOf(
                joinedPlan.id to mapOf(
                    MealCell(days[0], MealSlot.LUNCH) to "김치찌개",
                    MealCell(days[2], MealSlot.DINNER) to "카레",
                ),
            ),
        )
    }

    val currentPlan = CurrentPlan()

    // expect/actual: Android로 빌드하면 androidMain, iOS로 빌드하면 iosMain의 구현이 들어온다.
    val socialLogin: SocialLogin = platformSocialLogin()
}
