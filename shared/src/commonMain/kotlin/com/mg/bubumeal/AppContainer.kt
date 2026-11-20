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
    // 내 식단표에는 동반자가 있는 것으로 시작한다. 설정에서 해제하면 발급 버튼이 보이는 것을 확인할 수 있다.
    private val myPlan = PlanSummary(PlanId("my-plan"), PlanRole.OWNER, hasCompanion = true)
    private val joinedPlan = PlanSummary(PlanId("joined-plan"), PlanRole.COMPANION, hasCompanion = true)

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
            // 목록에서 빠진(삭제, 나가기, 접근 상실) 식단표의 끼니를 읽거나 쓰면 접근 상실이 된다.
            isAccessible = fakePlanRepository::isAccessible,
        )
    }

    /** 디버그용: 상대가 식단표를 지운 것처럼 접근을 잃게 한다. 서버가 나오면 null로 두고 지운다. */
    val debugLoseAccess: ((PlanId) -> Unit)? = fakePlanRepository::loseAccess

    val currentPlan = CurrentPlan()

    // expect/actual: Android로 빌드하면 androidMain, iOS로 빌드하면 iosMain의 구현이 들어온다.
    val socialLogin: SocialLogin = platformSocialLogin()
}
