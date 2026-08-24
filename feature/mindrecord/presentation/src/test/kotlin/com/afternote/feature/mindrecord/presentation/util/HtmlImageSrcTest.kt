package com.afternote.feature.mindrecord.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    @Test
    fun `스킴이 없는 값은 첫 경로 세그먼트를 잃지 않는다`() {
        // substringAfter("://") 는 구분자가 없으면 원문을 그대로 돌려주고, 이어지는
        // substringAfter('/') 가 mindrecords 를 떼 버린다 — 조용히 틀린 키가 나간다.
        assertEquals(
            "mindrecords/staging/13/a.png",
            "mindrecords/staging/13/a.png".toUploadedFileKey(),
        )
    }
}

/**
 * 목록 카드 미리보기에 대체 문자가 새지 않는지 (#549 리뷰 지적).
 *
 * `HtmlCompat.fromHtml` 은 ImageGetter 없이 파싱하면 `img` 자리에 U+FFFC(OBJECT
 * REPLACEMENT CHARACTER)를 남긴다. 본문에 `img` 를 정식으로 넣기 시작하면서 드러나는
 * 자리라, 카드 미리보기 둘째 줄이 통째로 `￼` 가 된다. 공백이 아니라 `trim()` 으로는
 * 지워지지 않는다.
 *
 * `HtmlCompat` 을 타서 Robolectric 이 필요하다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HtmlPlainTextTest {
    @Test
    fun `본문 뒤 이미지는 미리보기에 대체 문자를 남기지 않는다`() {
        val html = "<p>본문</p><p><img src=\"https://cdn.example.com/a.png\" /></p>"

        assertEquals("본문", html.htmlToPlainText())
    }

    @Test
    fun `이미지만 있는 본문은 미리보기가 비어 있다`() {
        assertEquals("", "<img src=\"https://cdn.example.com/a.png\" />".htmlToPlainText())
    }

    @Test
    fun `글과 이미지가 섞여 있어도 글만 남는다`() {
        val html = "<p>앞</p><img src=\"https://cdn.example.com/a.png\" /><p>뒤</p>"

        assertEquals(false, html.htmlToPlainText().contains('￼'))
    }
}
