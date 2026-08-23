package com.afternote.feature.mindrecord.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 본문 HTML 에서 목록 카드 썸네일을 뽑는 규칙 (#549).
 *
 * 데일리질문의 요청/응답 계약에 `imageUrl` 이 없어, 썸네일의 출처는 별도 필드가 아니라
 * `content` 다. 에디터가 넣는 형태와 서버 sanitize 를 거친 형태 모두에서 뽑혀야 한다.
 */
class HtmlImageSrcTest {
    @Test
    fun `에디터가 삽입하는 형태에서 뽑는다`() {
        // WriteTextField 가 업로드 URL 을 이 형태로 본문 끝에 붙인다.
        val html = "<p>사진과 함께</p><img src=\"https://cdn.example.com/a.png\" />"

        assertEquals("https://cdn.example.com/a.png", html.firstHtmlImageSrcOrNull())
    }

    @Test
    fun `서버 sanitize 를 거쳐 alt 가 붙은 형태에서도 뽑는다`() {
        // 실서버 왕복 실측(2026-08-23) 형태 — src 앞뒤로 다른 속성이 붙는다.
        val html =
            "<p>본문</p><p><img src=\"https://afternote-bucket.s3.amazonaws.com/probe.png\" alt=\"사진\" /></p>"

        assertEquals("https://afternote-bucket.s3.amazonaws.com/probe.png", html.firstHtmlImageSrcOrNull())
    }

    @Test
    fun `src 가 뒤쪽 속성이어도 뽑는다`() {
        val html = "<img alt=\"사진\" width=\"100\" src=\"https://cdn.example.com/b.png\" />"

        assertEquals("https://cdn.example.com/b.png", html.firstHtmlImageSrcOrNull())
    }

    @Test
    fun `작은따옴표도 허용한다`() {
        assertEquals("https://cdn.example.com/c.png", "<img src='https://cdn.example.com/c.png'>".firstHtmlImageSrcOrNull())
    }

    @Test
    fun `여러 장이면 첫 장을 쓴다`() {
        val html = "<img src=\"https://cdn.example.com/1.png\"><img src=\"https://cdn.example.com/2.png\">"

        assertEquals("https://cdn.example.com/1.png", html.firstHtmlImageSrcOrNull())
    }

    @Test
    fun `이미지가 없으면 null 이라 텍스트 카드로 그린다`() {
        assertNull("<p>글만 있는 본문</p>".firstHtmlImageSrcOrNull())
    }

    @Test
    fun `img 를 흉내 낸 다른 태그에는 걸리지 않는다`() {
        // `<image>`·`<imgx>` 같은 이름은 img 가 아니다 — 단어 경계로 막는다.
        assertNull("<imgx src=\"https://cdn.example.com/x.png\">".firstHtmlImageSrcOrNull())
    }

    @Test
    fun `src 가 비어 있으면 썸네일로 쓰지 않는다`() {
        // 빈 문자열을 넘기면 이미지 카드 레이아웃으로 그려 놓고 아무것도 안 뜬다.
        assertNull("<img src=\"\" alt=\"사진\">".firstHtmlImageSrcOrNull())
    }

    @Test
    fun `업로드 URL 은 스킴과 호스트를 떼 fileKey 가 된다`() {
        // 서버가 본문 img src 에서 기대하는 형태 — 전체 URL 을 넣으면 호스트를 한 번 더 붙인다.
        val url = "https://dpy1u4p7yz7bh.cloudfront.net/mindrecords/staging/13/a.png"

        assertEquals("mindrecords/staging/13/a.png", url.toUploadedFileKey())
    }

    @Test
    fun `호스트가 바뀌어도 경로 규칙을 코드에 박지 않는다`() {
        // 디렉터리 구조를 서버가 바꿔도 스킴·호스트만 떼는 규칙은 그대로 따라간다.
        val url = "https://cdn.example.net/some/other/layout/b.png"

        assertEquals("some/other/layout/b.png", url.toUploadedFileKey())
    }
}
