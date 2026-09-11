package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.util.RecordContentBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 상세 화면 헤더 이미지 선택 규칙 (#759).
 *
 * 시안 4종은 첨부 이미지 유무로만 갈린다. 서버는 "대표 이미지" 를 따로 주지 않으므로
 * 본문 HTML 에서 **첫 이미지**를 뽑아 헤더로 쓰고, 하나도 없으면 그라데이션 변형이 된다.
 *
 * 뽑는 함수를 직접 부르지 않고 **ViewModel 이 내놓는 `heroImageUrl`** 로 본다 — 규칙이
 * 맞아도 상태에 실리지 않으면 헤더는 여전히 비어 있다 (#1674). 본문 분할이 `HtmlCompat`
 * 을 타서 Robolectric 이 필요하다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordDetailHeroImageTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `본문 첫 이미지를 헤더로 쓴다`() {
        val state =
            detailOf(
                "<p>앞 문단입니다.</p>" +
                    "<img src=\"https://cdn.example.com/first.png\" />" +
                    "<img src=\"https://cdn.example.com/second.png\" />",
            )

        assertEquals("https://cdn.example.com/first.png", state.heroImageUrl)
    }

    @Test
    fun `이미지가 글 중간에 있어도 헤더로 올린다`() {
        // 헤더는 본문 순서와 무관하다 — 첫 문단 뒤에 붙은 사진도 시안의 "이미지 O" 변형이다.
        val state = detailOf("<p>앞 문단</p><img src=\"https://cdn.example.com/mid.png\" /><p>뒤 문단</p>")

        assertEquals("https://cdn.example.com/mid.png", state.heroImageUrl)
    }

    @Test
    fun `이미지가 없으면 헤더 이미지도 없다`() {
        val state = detailOf("<p>글만 있는 본문입니다.</p>")

        assertNull(state.heroImageUrl)
        assertEquals(1, state.blocks.count { it is RecordContentBlock.Text })
    }

    @Test
    fun `본문이 비어도 터지지 않는다`() {
        assertNull(detailOf("").heroImageUrl)
    }

    @Test
    fun `src 가 빈 img 는 헤더로 쓰지 않는다`() {
        // 빈 src 를 그대로 넘기면 헤더가 "이미지 O" 변형으로 그려진 뒤 영원히 비어 있다.
        assertNull(detailOf("<p>본문</p><img src=\"\" />").heroImageUrl)
    }

    private fun detailOf(content: String): RecordDetailUiState.Success {
        lateinit var state: RecordDetailUiState
        runTest(dispatcher) {
            val viewModel =
                RecordDetailViewModel(
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf("recordId" to RECORD_ID, "isDiary" to true, "yearMonth" to "2026-09"),
                        ),
                    diaryRepository = FakeDiaryRepository(initialDiaries = listOf(diary(content))),
                    dailyQuestionRepository = FakeDailyQuestionRepository(),
                    errorReporter = RecordingErrorReporter(),
                )
            advanceUntilIdle()
            state = viewModel.uiState.value
        }
        return state as RecordDetailUiState.Success
    }

    private fun diary(content: String) =
        Diary(
            diaryId = RECORD_ID,
            title = "오늘의 산책",
            content = content,
            date = "2026-09-03",
            createdAt = "2026.09.03 목",
            todayMood = TodayMood.HAPPY,
        )

    private companion object {
        const val RECORD_ID = 1L
    }
}
