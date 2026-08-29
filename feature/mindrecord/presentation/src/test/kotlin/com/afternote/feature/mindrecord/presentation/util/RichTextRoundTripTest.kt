package com.afternote.feature.mindrecord.presentation.util

import com.afternote.feature.mindrecord.presentation.util.escapeHtml
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

    @Test
    fun `첨부 파일명의 꺾쇠는 마크업으로 살아나지 않는다`() {
        // `displayName` 은 파일을 넘긴 앱(content provider)이 정하는 값이다. 이름 속 태그를
        // 그대로 넣으면 링크 텍스트가 마크업으로 살아난다 (#1067 리뷰).
        //
        // `alt` 쪽은 같은 이유로 함께 이스케이프하지만 **여기서 단언하지 않는다** — 이 에디터가
        // `alt` 를 직렬화에서 통째로 버려, 이스케이프 유무와 무관하게 왕복 결과가 같다(실측).
        // 실패할 수 없는 단언을 이름만 붙여 두면 다음 사람이 «가드가 있다» 로 읽는다.
        val name = "<b>계약서</b>.pdf"

        val escaped = roundTrip("""<a href="k">${name.escapeHtml()}</a>""")

        // 이스케이프한 이름은 텍스트로 되돌아온다 — 태그로 해석되면 링크 안에 <b> 가 생긴다.
        assertTrue("이름이 마크업이 됐다: $escaped", !escaped.contains("<b>"))
    }

    private fun roundTrip(html: String): String =
        RichTextState()
            .apply { setHtml(html) }
            .toHtml()
}
