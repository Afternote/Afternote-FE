package com.afternote.feature.mindrecord.presentation.screen.sender

import com.afternote.feature.mindrecord.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 주간리포트 요약 문구의 공백을 **컴파일된 리소스**로 고정한다 (#732).
 *
 * 소스 XML 만 보면 통과하는 종류의 결함이다 — 조각 리소스에 앞뒤 공백을 두면 aapt2 가 그것을
 * 지워 APK 에서만 "이번 주,박서연님은3일의…" 로 붙는다. 그래서 Robolectric 으로 실제 리소스를
 * 읽어 확인한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeeklyReportSummaryTest {
    private val context: android.content.Context = org.robolectric.RuntimeEnvironment.getApplication()

    @Test
    fun `요약 문구는 시안대로 공백을 유지한다`() {
        val daysText = context.getString(R.string.mindrecord_weekly_report_days_format, 3)
        val sentence = context.getString(R.string.mindrecord_weekly_report_recorded, "박서연", daysText)

        assertEquals("이번 주, 박서연 님은 3일의 마음을 기록하셨네요.", sentence)
    }

    @Test
    fun `이름과 기록일수 구간을 각각 찾는다`() {
        val sentence = "이번 주, 박서연 님은 3일의 마음을 기록하셨네요."

        val highlights = recordedSummaryHighlights(sentence, userName = "박서연", daysText = "3일")

        assertEquals(2, highlights.size)
        assertEquals("박서연", sentence.substring(highlights[0].first, highlights[0].last + 1))
        assertEquals("3일", sentence.substring(highlights[1].first, highlights[1].last + 1))
    }

    @Test
    fun `이름에 기록일수와 같은 문자열이 들어 있어도 구간이 겹치지 않는다`() {
        val sentence = "이번 주, 3일 님은 3일의 마음을 기록하셨네요."

        val highlights = recordedSummaryHighlights(sentence, userName = "3일", daysText = "3일")

        assertEquals(2, highlights.size)
        // 이름은 첫 번째 "3일", 기록일수는 그 뒤의 "3일" — 같은 구간을 두 번 잡지 않는다.
        assertEquals(6, highlights[0].first)
        assertEquals(12, highlights[1].first)
    }

    @Test
    fun `이름이 비어 있으면 기록일수만 강조한다`() {
        val sentence = "이번 주,  님은 3일의 마음을 기록하셨네요."

        val highlights = recordedSummaryHighlights(sentence, userName = "", daysText = "3일")

        assertEquals(1, highlights.size)
        assertEquals("3일", sentence.substring(highlights[0].first, highlights[0].last + 1))
    }
}
