package com.afternote.feature.mindrecord.presentation.screen.memoryspace

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.GetMemorySpaceUseCase
import com.afternote.feature.mindrecord.presentation.viewmodel.MemorySpaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * 추억 공간의 카드 → 상세 오버레이 → 닫기 → 돌아가기 왕복 (#1693).
 *
 * 종전에는 app 계측 테스트였다. 집계 UseCase 와 카드 모델이 이 모듈 안으로 닫히면서
 * 조립도 계약 옆으로 왔다 — app 이 이 화면을 손으로 조립할 이유가 없다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class MemorySpaceDetailOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `카드를 열면 본문과 기분 태그가 뜨고 닫은 뒤 돌아가기가 화면을 넘긴다`() {
        val memoryDate = LocalDate.now()
        val memory =
            Diary(
                diaryId = 501L,
                title = "추억이 된 하루",
                content = "이 순간은 나에게 특별한 의미가 있었습니다.",
                date = memoryDate.toString(),
                createdAt = memoryDate.toString(),
                todayMood = TodayMood.HAPPY,
                imageUrl = "https://afternote.test/memory.jpg",
                isDraft = false,
            )
        val diaryRepository =
            FakeDiaryRepository(
                onGetList = { yearMonth, _ ->
                    val diaries =
                        if (yearMonth == YearMonth.from(memoryDate).toString()) listOf(memory) else emptyList()
                    Result.success(
                        DiaryList(
                            diaries = diaries,
                            monthDiaryCount = diaries.size,
                            weeklyDominantMood = diaries.firstOrNull()?.todayMood,
                        ),
                    )
                },
            )
        val viewModel =
            MemorySpaceViewModel(
                getMemorySpace =
                    GetMemorySpaceUseCase(
                        diaryRepository = diaryRepository,
                        dailyQuestionRepository = FakeDailyQuestionRepository(),
                    ),
                errorReporter = RecordingErrorReporter(),
            )
        var backCalls = 0

        composeRule.setContent {
            AfternoteTheme {
                MemorySpaceScreen(viewModel = viewModel, onBackClick = { backCalls += 1 })
            }
        }

        // 제목은 상단바와 본문 헤더 두 곳에 있다 — 화면이 떴다는 확인이면 첫 노드로 족하다.
        composeRule.onAllNodesWithText("MEMORY SPACE").onFirst().assertIsDisplayed()
        composeRule.waitUntil {
            composeRule.onAllNodesWithContentDescription("추억이 된 하루").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("추억이 된 하루").performClick()
        composeRule
            .onNodeWithText("이 순간은 나에게 특별한 의미가 있었습니다.", substring = true)
            .assertIsDisplayed()
        // 태그는 사용자가 고른 오늘의 기분 이모지다 — 종전 더미의 `#평온` 은 출처가 없었다 (#559).
        composeRule.onNodeWithText("#😊").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("닫기").performClick()
        composeRule
            .onNodeWithText("이 순간은 나에게 특별한 의미가 있었습니다.", substring = true)
            .assertDoesNotExist()

        composeRule.onNodeWithText("돌아가기").performClick()
        composeRule.runOnIdle { assertEquals(1, backCalls) }
    }
}
