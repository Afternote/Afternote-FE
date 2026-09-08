package com.afternote.feature.home.presentation

import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.User
import com.afternote.feature.home.presentation.usecase.GetHomeSummaryUseCase
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.usecase.GetWeeklyRecordCountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * 느린 주간 수 조회가 홈 전체를 붙잡지 못하는지 (#562 리뷰).
 *
 * 주간 수만 `/mind-record` 에서 오는데 그 엔드포인트가 병적으로 느리다(#1122). 이 값을
 * 끝까지 기다리면 이름·오늘의 질문·NEXT STEP 이 이미 도착해 있어도 홈 전체가 그 동안
 * shimmer 로 남는다 — 실기동에서 **60,179ms 뒤 SocketTimeout** 까지 갔다.
 *
 * 예산을 넘긴 주간 수는 «못 불러옴»(null) 과 같이 다룬다. 그리드가 이미 그 상태를 그리므로
 * 새 상태가 늘지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeWeeklyCountBudgetTest {
    @Test
    fun `주간 수가 느려도 홈은 나머지 응답 속도로 뜬다`() =
        runTest {
            // 서버가 읽기 타임아웃(60초)까지 답하지 않는 경우다.
            val useCase = useCase(weeklyDelayMillis = 60_000)

            val summary = useCase().getOrThrow()

            assertTrue("홈이 주간 수를 끝까지 기다렸다: ${currentTime}ms", currentTime < 10_000)
            assertEquals("효기", summary.userName)
            assertNull("예산을 넘긴 주간 수는 «못 불러옴» 으로 다룬다", summary.weeklyRecordCount)
        }

    @Test
    fun `예산 안에 오면 그 값을 쓴다`() =
        runTest {
            // 예산이 값을 버리는 쪽으로만 작동하면 «항상 미상» 이 되고 #562 가 되돌아간다.
            val useCase = useCase(weeklyDelayMillis = 100)

            assertEquals(3, useCase().getOrThrow().weeklyRecordCount)
        }

    private fun useCase(weeklyDelayMillis: Long): GetHomeSummaryUseCase =
        GetHomeSummaryUseCase(
            myProfileRepository = StubMyProfileRepository,
            userReceiverRepository = StubUserReceiverRepository,
            dailyQuestionRepository = StubDailyQuestionRepository,
            getWeeklyRecordCount = GetWeeklyRecordCountUseCase(SlowWeeklyReportRepository(weeklyDelayMillis)),
        )
}

private class SlowWeeklyReportRepository(
    private val delayMillis: Long,
) : WeeklyReportRepository {
    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
        delay(delayMillis)
        return Result.success(
            WeeklyReport(
                dailyQuestionAmount = 1,
                diaryAmount = 2,
                summaryText = "",
                week = emptyList(),
                dailyQuestions = emptyList(),
                emotions = emptyList(),
                emotionAnalysis = null,
            ),
        )
    }
}

// 두 계약 다 이 시나리오가 타는 호출 하나씩만 답한다. 프록시가 무는 표면이 곧 계약 크기라,
// 다른 멤버가 호출되면 `error` 로 떨어진다 (#1741·#1742).
private val StubMyProfileRepository: MyProfileRepository =
    Proxy.newProxyInstance(
        MyProfileRepository::class.java.classLoader,
        arrayOf(MyProfileRepository::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getMyProfile" -> User(name = "효기", email = "user@example.com", phone = null, profileImageUrl = null)
            else -> error("Unexpected call: ${method.name}")
        }
    } as MyProfileRepository

private val StubUserReceiverRepository: UserReceiverRepository =
    Proxy.newProxyInstance(
        UserReceiverRepository::class.java.classLoader,
        arrayOf(UserReceiverRepository::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getReceivers" -> emptyList<Receiver>()
            else -> error("Unexpected call: ${method.name}")
        }
    } as UserReceiverRepository

private object StubDailyQuestionRepository : DailyQuestionRepository {
    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> = Result.success(emptyList())

    override suspend fun getToday(): Result<TodayDailyQuestion> = Result.failure(IllegalStateException("이 테스트의 관심사가 아니다"))

    override suspend fun create(payload: DailyQuestionCreatePayload) = error("호출되면 안 됨")

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ) = error("호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
}
