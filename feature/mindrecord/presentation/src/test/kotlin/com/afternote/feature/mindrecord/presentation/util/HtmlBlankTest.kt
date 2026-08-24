package com.afternote.feature.mindrecord.presentation.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "화면이 비었는가" 판정 (#923).
 *
 * 리치 에디터는 아무것도 입력하지 않아도 `<p></p>` 를 내보내므로 `isBlank()` 로는 판정할
 * 수 없다. 이 값을 "사용자가 이미 썼다" 로 오해하면 이어쓸 임시저장 본문이 실리지 않는다.
 *
 * 반대 방향도 같은 무게다 — 태그를 통째로 걷으면 **태그 자체가 내용인** 본문(이미지·링크)이
 * 빈 것으로 접혀, 사진만 첨부한 상태가 도착한 draft 로 덮인다 (리뷰 지적).
 */
class HtmlBlankTest {
    @Test
    fun `빈 에디터가 내보내는 문단은 비어 있다`() {
        assertTrue("<p></p>".isHtmlBlank())
        assertTrue("".isHtmlBlank())
        assertTrue("<p><br></p>".isHtmlBlank())
    }

    @Test
    fun `글이 있으면 비어 있지 않다`() {
        assertFalse("<p>한 글자</p>".isHtmlBlank())
    }

    @Test
    fun `이미지만 있어도 비어 있지 않다`() {
        // 종전 판정에서는 태그를 걷어 빈 문자열이 됐다. 구 `isBlank()` 는 보호하던 입력이다.
        assertFalse("<img src=\"https://cdn.example.com/a.png\" />".isHtmlBlank())
        assertFalse("<p></p><img src=\"https://cdn.example.com/a.png\" />".isHtmlBlank())
    }

    @Test
    fun `다른 미디어 태그도 내용으로 본다`() {
        assertFalse("<video src=\"https://cdn.example.com/a.mp4\"></video>".isHtmlBlank())
    }

    @Test
    fun `가시 문자 엔티티만 있어도 비어 있지 않다`() {
        assertFalse("<p>&lt;</p>".isHtmlBlank())
        assertFalse("<p>&amp;</p>".isHtmlBlank())
    }

    @Test
    fun `공백 엔티티만 있으면 비어 있다`() {
        assertTrue("<p>&nbsp;</p>".isHtmlBlank())
        assertTrue("<p>&nbsp;&nbsp;</p>".isHtmlBlank())
    }

    @Test
    fun `img 를 흉내 낸 이름에는 걸리지 않는다`() {
        // `<image>`·`<imgx>` 는 img 가 아니다 — 단어 경계로 막는다.
        assertTrue("<p></p><imgx>".isHtmlBlank())
    }
}
