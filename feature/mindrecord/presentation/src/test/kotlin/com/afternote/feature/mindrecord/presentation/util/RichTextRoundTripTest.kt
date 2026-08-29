package com.afternote.feature.mindrecord.presentation.util

import com.mohamedrejeb.richeditor.model.RichTextState
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 이스케이프한 href 가 **에디터 왕복 뒤에도 같은 주소인지** 본다 (#1067 리뷰).
 *
 * 저장되는 값은 `WriteTextField` 의 `onValueChange(state.toHtml())` 이므로, 함수 반환값만 보는
 * [BodyLinkHrefTest] 로는 이 경계가 안 잡힌다. 리치 에디터는 속성값의 **숫자 문자 참조를
 * 디코드하지 않아**, `&#39;` 를 넣으면 그 `&` 를 다시 인코딩해 `&amp;#39;` 로 굳힌다 —
 * 뷰어가 풀면 사용자가 적지 않은 주소가 된다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RichTextRoundTripTest {
    @Test
    fun `홑따옴표가 든 주소는 왕복 뒤에도 그대로다`() {
        val href = requireNotNull("https://example.com/it's".toBodyLinkHrefOrNull())

        val roundTripped = roundTrip("""<a href="$href">link</a>""")

        assertTrue(
            "왕복 뒤 href 가 달라졌다: $roundTripped",
            roundTripped.contains("""href="https://example.com/it's""""),
        )
        // 이 문자열이 나오면 숫자 문자 참조가 굳은 것이다 — 목적지가 바뀐다.
        assertTrue("&amp;#39; 로 굳었다: $roundTripped", !roundTripped.contains("&amp;#39;"))
    }

    @Test
    fun `앰퍼샌드가 든 주소도 왕복 뒤에 누적되지 않는다`() {
        val href = requireNotNull("https://example.com/?a=1&b=2".toBodyLinkHrefOrNull())

        val roundTripped = roundTrip("""<a href="$href">link</a>""")

        assertTrue("&amp;amp; 로 누적됐다: $roundTripped", !roundTripped.contains("&amp;amp;"))
    }

    private fun roundTrip(html: String): String =
        RichTextState()
            .apply { setHtml(html) }
            .toHtml()
}
