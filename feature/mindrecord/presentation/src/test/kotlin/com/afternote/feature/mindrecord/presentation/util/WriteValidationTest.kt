package com.afternote.feature.mindrecord.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 작성 화면 입력 검증 가드 (#722).
 *
 * 두 가지가 그대로 통과하고 있었다.
 * - 리치 에디터가 빈 문단을 `<p></p>`·`<br>` 로 직렬화해, 화면이 비어 있어도
 *   `isNotBlank()` 검증을 통과했다 (빈 답변 저장 후 화면 이탈).
 * - 링크 입력이 임의 문자열을 허용해 그대로 `a` 태그의 `href` 가 됐다.
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
    fun `http 와 https 주소만 링크로 받는다`() {
        assertTrue("https://example.com".isSupportedLinkUrl())
        assertTrue("http://example.com/path?q=1".isSupportedLinkUrl())
    }

    @Test
    fun `형식이 아닌 값은 링크로 받지 않는다`() {
        // 종전에는 이런 값도 그대로 href 가 됐다.
        listOf("", "  ", "그냥 텍스트", "example.com", "ftp://example.com", "https://", "https:// example.com")
            .forEach { raw -> assertFalse("거부해야 한다: $raw", raw.isSupportedLinkUrl()) }
    }

    @Test
    fun `자바스크립트 스킴은 거부한다`() {
        assertFalse("javascript:alert(1)".isSupportedLinkUrl())
    }

    @Test
    fun `속성에 넣기 전에 이스케이프한다`() {
        // 검증을 통과해도 따옴표·꺾쇠가 섞이면 a 태그를 깨고 나올 수 있다.
        assertEquals(
            "https://example.com/?a=1&amp;b=&lt;x&gt;&quot;&#39;",
            "https://example.com/?a=1&b=<x>\"'".escapeHtml(),
        )
    }
}
