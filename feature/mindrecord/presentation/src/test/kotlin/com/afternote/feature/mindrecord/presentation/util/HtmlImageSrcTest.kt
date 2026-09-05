package com.afternote.feature.mindrecord.presentation.util

import com.mohamedrejeb.richeditor.model.RichTextState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `본문의 업로드 URL 을 서버가 준 fileKey 로 바꾼다`() {
        val url = "https://cdn.example.net/mindrecords/staging/13/a.png"
        val html = """<p>앞</p><img src="$url" alt="사진"><p>뒤</p>"""

        val wire = html.toWireContent(mapOf(url to "mindrecords/staging/13/a.png"))

        assertEquals("""<p>앞</p><img src="mindrecords/staging/13/a.png" alt="사진"><p>뒤</p>""", wire)
    }

    @Test
    fun `CDN 이 경로 프리픽스를 붙여도 서버가 준 키가 그대로 나간다`() {
        // **역산이 틀리던 자리다** (#1125). 스킴·호스트만 떼는 종전 규칙은 여기서
        // `bucket-7/mindrecords/staging/13/a.png` 를 만들어 조용히 틀린 키를 보냈다.
        // 지금은 presigned 응답이 준 값을 그대로 쓰므로 프리픽스가 몇 겹이든 영향이 없다.
        val url = "https://cdn.example.net/bucket-7/mindrecords/staging/13/a.png"

        val wire = """<img src="$url">""".toWireContent(mapOf(url to "mindrecords/staging/13/a.png"))

        assertEquals("""<img src="mindrecords/staging/13/a.png">""", wire)
    }

    @Test
    fun `이번에 업로드하지 않은 영구 URL 은 건드리지 않는다`() {
        // 이미 저장된 본문의 영구 URL 은 서버가 그대로 통과시킨다 — 키로 바꾸면 이미 옮겨진
        // 파일을 다시 옮기려다 실패한다 (#549).
        val permanent = "https://cdn.example.net/mindrecords/permanent/13/old.png"
        val uploaded = "https://cdn.example.net/mindrecords/staging/13/new.png"
        val html = """<img src="$permanent"><img src="$uploaded">"""

        val wire = html.toWireContent(mapOf(uploaded to "mindrecords/staging/13/new.png"))

        assertEquals("""<img src="$permanent"><img src="mindrecords/staging/13/new.png">""", wire)
    }

    @Test
    fun `쿼리가 둘 이상인 URL 도 에디터 왕복 뒤에 치환된다`() {
        // **리치 에디터가 직렬화하면서 `&` 를 `&amp;` 로 바꾼다**(실측). 원문 URL 로 그대로
        // 찾으면 여기서 못 맞춰 전체 URL 이 서버로 나가고, #549 의 이중 호스트·403 이 재발한다
        // (#1125 리뷰). 그래서 이 가드는 **실제 왕복을 거친 HTML** 로 본다.
        val url = "https://cdn.example.net/mindrecords/staging/13/a.png?x=1&y=2"
        val serialized = RichTextState().apply { setHtml("""<img src="$url">""") }.toHtml()
        assertTrue("왕복 전제가 깨졌다: $serialized", "&amp;" in serialized)

        val wire = serialized.toWireContent(mapOf(url to "mindrecords/staging/13/a.png"))

        assertTrue("전체 URL 이 그대로 나갔다: $wire", "cdn.example.net" !in wire)
        assertTrue("fileKey 로 안 바뀌었다: $wire", """src="mindrecords/staging/13/a.png"""" in wire)
    }

    @Test
    fun `링크 href 도 같은 규칙으로 치환된다`() {
        // 첨부 파일은 `a href` 로 들어간다 — `img src` 와 같은 규칙이 적용되는 것을 실서버로
        // 확인했다(#549). 왕복 escape 도 똑같이 걸린다.
        val url = "https://cdn.example.net/mindrecords/staging/13/b.m4a?v=1&t=2"
        val serialized = RichTextState().apply { setHtml("""<a href="$url">음성</a>""") }.toHtml()

        val wire = serialized.toWireContent(mapOf(url to "mindrecords/staging/13/b.m4a"))

        assertTrue("전체 URL 이 그대로 나갔다: $wire", "cdn.example.net" !in wire)
        assertTrue("fileKey 로 안 바뀌었다: $wire", """href="mindrecords/staging/13/b.m4a"""" in wire)
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
