package com.afternote.feature.mindrecord.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 작성 화면 입력 검증 가드 (#722).
 *
 * 두 가지가 그대로 통과하고 있었다.
 * - 리치 에디터가 빈 문단을 `<p></p>`·`<br>` 로 직렬화해, 화면이 비어 있어도
 *   `isNotBlank()` 검증을 통과했다 (빈 답변 저장 후 화면 이탈).
 * - 링크 입력이 임의 문자열을 허용해 그대로 `a` 태그의 `href` 가 됐다.
 *
 * 링크 쪽 검사는 #1067 에서 [toBodyLinkHrefOrNull] 로 옮겨졌다 — 종전 `isSupportedLinkUrl` 은
 * `java.net.URI` 가 비ASCII 에서 던져 **한글 도메인·한글 경로를 정상 주소인데도 거부**했다.
 * 그래서 「받는가」가 아니라 **「본문에 넣을 형태로 무엇을 돌려주는가」**를 본다.
 */
class WriteValidationTest {
    @Test
    fun `빈 문단은 내용이 없는 것으로 본다`() {
        // compose-rich-editor 가 실제로 내보내는 형태들.
        listOf("", "   ", "<p></p>", "<br>", "<p><br></p>", "<p>&nbsp;</p>").forEach { html ->
            assertTrue("비어 있어야 한다: $html", html.isHtmlBlank())
        }
    }

    @Test
    fun `글자가 하나라도 있으면 내용이 있는 것으로 본다`() {
        listOf("<p>ㄱ</p>", "<p><strong>안녕</strong></p>", "본문").forEach { html ->
            assertFalse("내용이 있어야 한다: $html", html.isHtmlBlank())
        }
    }

    @Test
    fun `http 와 https 주소는 그대로 통과한다`() {
        assertEquals("https://example.com", "https://example.com".toBodyLinkHrefOrNull())
        assertEquals("http://example.com/path?q=1", "http://example.com/path?q=1".toBodyLinkHrefOrNull())
    }

    @Test
    fun `스킴을 안 적으면 https 로 읽는다`() {
        // #722 시절에는 거부했다. 호스트부터 적는 입력이 흔한데 그걸 막으면 정상 사용을 막는다 (#1067).
        assertEquals("https://example.com", "example.com".toBodyLinkHrefOrNull())
        // 콜론 뒤가 전부 숫자면 스킴 선언이 아니라 host:port 다.
        assertEquals("https://example.com:8080", "example.com:8080".toBodyLinkHrefOrNull())
    }

    @Test
    fun `형식이 아닌 값은 링크로 받지 않는다`() {
        // 종전에는 이런 값도 그대로 href 가 됐다.
        listOf("", "  ", "ftp://example.com", "https://", "https:// example.com")
            .forEach { raw -> assertNull("거부해야 한다: $raw", raw.toBodyLinkHrefOrNull()) }
    }

    @Test
    fun `자바스크립트 스킴은 거부한다`() {
        // 스킴을 적었으면 그 값을 존중한다 — https 를 덧붙여 «정상 주소» 로 둔갑시키지 않는다.
        assertNull("javascript:alert(1)".toBodyLinkHrefOrNull())
    }

    @Test
    fun `한글 도메인과 한글 경로는 거부하지 않는다`() {
        // 종전 `isSupportedLinkUrl` 이 URI 파싱으로 통째로 거부하던 자리다 (#1067).
        assertEquals("https://xn--3e0bp5xtnuo2a.xn--3e0b707e", "한국문화.한국".toBodyLinkHrefOrNull())
    }

    @Test
    fun `속성에 넣기 전에 이스케이프한다`() {
        // 검증을 통과해도 따옴표·꺾쇠가 섞이면 a 태그를 깨고 나올 수 있다.
        //
        // `'` 는 일부러 바꾸지 않는다 — 큰따옴표 속성 안에서 홑따옴표는 경계를 못 만들고,
        // 리치 에디터가 속성값의 숫자 문자 참조를 디코드하지 않아 `&#39;` 를 넣으면 `&amp;#39;` 로
        // 굳어 **저장되는 주소가 달라진다** (#1067 리뷰).
        assertEquals(
            "https://example.com/?a=1&amp;b=&lt;x&gt;&quot;'",
            "https://example.com/?a=1&b=<x>\"'".escapeHtml(),
        )
    }
}
