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
    fun `첨부 파일명이 alt 속성을 닫고 본문에 요소를 심지 못한다`() {
        // **이 파일에서 가장 값나가는 가드다.** `displayName` 은 파일을 넘긴 앱(content provider)이
        // 정하는 값인데, 이름 하나로 `alt` 를 닫고 그 뒤에 요소를 이어 붙이면 **수신자가 나중에
        // 열람할 본문에 외부 링크가 심긴다** (#1067 리뷰).
        //
        // 에디터가 `alt` 자체는 버리지만, 속성이 닫힌 뒤의 문자열은 **본문 요소로 살아남는다.**
        // 실측:
        //
        //     RAW <p><img src="k" width="0.0" …><a href="https://evil.com" …>여기</a><img …></p>
        //     ESC <p><img src="k" width="320.0" height="240.0"></p>
        val name = """x"><a href="https://evil.com">여기</a><img alt=""""

        val escaped = roundTrip("""<img src="k" alt="${name.escapeHtml()}" width="320" height="240" />""")

        assertTrue("외부 링크가 본문에 심겼다: $escaped", !escaped.contains("evil.com"))
        // 원래 이미지도 온전해야 한다 — 속성이 닫히면 width/height 가 값으로 먹혀 0.0 이 된다.
        assertTrue("이미지가 뭉개졌다: $escaped", escaped.contains("width=\"320"))
    }

    @Test
    fun `첨부 파일명의 꺾쇠는 링크 텍스트에서 마크업으로 살아나지 않는다`() {
        // 위와 같은 출처(content provider)의 값이 링크 텍스트 자리로 들어가는 경우다.
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
