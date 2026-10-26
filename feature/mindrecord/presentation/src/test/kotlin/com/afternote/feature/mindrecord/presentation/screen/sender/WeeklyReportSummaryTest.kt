package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.ObserveWeeklyReportUseCase
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 주간리포트 요약 문구의 공백과 강조를 **화면에서** 고정한다 (#732).
 *
 * 소스 XML 만 보면 통과하는 종류의 결함이다 — 조각 리소스에 앞뒤 공백을 두면 aapt2 가 그것을
 * 지워 APK 에서만 "이번 주,박서연님은3일의…" 로 붙는다. 그래서 Robolectric 으로 실제 리소스를
 * 읽어 확인한다.
 *
 * 강조 구간은 내부 도우미를 부르지 않고 **화면이 실제로 그린 `AnnotatedString`** 에서 읽는다 —
 * 구간 계산이 맞아도 그 값이 스타일로 실리지 않으면 화면은 강조 없이 보인다 (#1674).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeeklyReportSummaryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: android.content.Context get() = composeRule.activity

    @Test
    fun `요약 문구는 시안대로 공백을 유지한다`() {
        val daysText = context.getString(R.string.mindrecord_weekly_report_days_format, 3)
        val sentence = context.getString(R.string.mindrecord_weekly_report_recorded, "박서연", daysText)

        assertEquals("이번 주, 박서연 님은 3일의 마음을 기록하셨네요.", sentence)
    }

    @Test
    fun `이름과 기록일수 구간을 각각 강조한다`() {
        val highlighted = highlightedTexts(userName = "박서연", recordedDays = 3)

        assertEquals(listOf("박서연", "3일"), highlighted)
    }

    @Test
    fun `이름에 기록일수와 같은 문자열이 들어 있어도 구간이 겹치지 않는다`() {
        // 이름은 첫 번째 "3일", 기록일수는 그 뒤의 "3일" — 같은 구간을 두 번 잡지 않는다.
        val sentence = summarySentence(userName = "3일", recordedDays = 3)
        val ranges = highlightedRanges(userName = "3일", recordedDays = 3)

        assertEquals(2, ranges.size)
        assertEquals(sentence.indexOf("3일"), ranges[0].first)
        assertEquals(sentence.indexOf("3일", startIndex = ranges[0].second), ranges[1].first)
    }

    @Test
    fun `이름이 비어 있으면 기록일수만 강조한다`() {
        val highlighted = highlightedTexts(userName = "", recordedDays = 3)

        assertEquals(listOf("3일"), highlighted)
    }

    private fun summarySentence(
        userName: String,
        recordedDays: Int,
    ): String {
        val daysText = context.getString(R.string.mindrecord_weekly_report_days_format, recordedDays)
        return context.getString(R.string.mindrecord_weekly_report_recorded, userName, daysText)
    }

    private fun highlightedTexts(
        userName: String,
        recordedDays: Int,
    ): List<String> {
        val sentence = summarySentence(userName, recordedDays)
        return highlightedRanges(userName, recordedDays).map { (start, end) -> sentence.substring(start, end) }
    }

    /** 화면이 그린 요약 문장에서 **강조색이 실린** 구간만 뽑는다. */
    private fun highlightedRanges(
        userName: String,
        recordedDays: Int,
    ): List<Pair<Int, Int>> {
        var highlightColor = Color.Unspecified
        renderScreen(userName = userName, recordedDays = recordedDays) { highlightColor = it }

        val sentence = summarySentence(userName, recordedDays)
        val node = composeRule.onNodeWithText(sentence).fetchSemanticsNode()
        val annotated: AnnotatedString = node.config[SemanticsProperties.Text].first()
        return annotated.spanStyles
            .filter { it.item.color == highlightColor }
            .map { it.start to it.end }
    }

    private fun renderScreen(
        userName: String,
        recordedDays: Int,
        onHighlightColor: (Color) -> Unit,
    ) {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        val viewModel =
            WeeklyReportViewModel(
                observeWeeklyReport =
                    ObserveWeeklyReportUseCase(
                        repository = SummaryWeeklyReportRepository(report(monday, recordedDays)),
                        userRepository =
                            FakeUserRepository.strict().apply {
                                onReceiverListFlow = { flowOf(emptyList()) }
                                onGetMyProfile = {
                                    User(name = userName, email = "a@b.c", phone = null, profileImageUrl = null)
                                }
                            },
                    ),
                changeTracker = MindRecordChangeTracker(),
                errorReporter = RecordingErrorReporter(),
            )
        composeRule.setContent {
            AfternoteTheme {
                val color = AfternoteDesign.colors.b1
                SideEffect { onHighlightColor(color) }
                WeeklyReportScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
    }

    /** 그 주 안에서 서로 다른 [recordedDays] 일에 기록이 있는 리포트. */
    private fun report(
        monday: LocalDate,
        recordedDays: Int,
    ) = WeeklyReport(
        dailyQuestionAmount = 0,
        diaryAmount = recordedDays,
        summaryText = "요약",
        week =
            (0 until recordedDays).map { offset ->
                WeeklyReportDay(
                    diaryId = offset.toLong(),
                    day = monday.plusDays(offset.toLong()).dayOfMonth,
                    isDiary = true,
                    countsAsRecord = true,
                    emotion = null,
                )
            },
        dailyQuestions = emptyList(),
        emotions = emptyList(),
        emotionAnalysis = null,
    )
}

private class SummaryWeeklyReportRepository(
    private val report: WeeklyReport,
) : WeeklyReportRepository {
    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> = Result.success(report)
}
