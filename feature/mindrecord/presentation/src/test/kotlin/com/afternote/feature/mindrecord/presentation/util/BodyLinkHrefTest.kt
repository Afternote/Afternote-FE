package com.afternote.feature.mindrecord.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 본문 링크로 넣어도 되는 주소만 통과시킨다 (#1067).
 *
 * 종전에는 검증이 아예 없어 입력 문자열이 `<a href="$url">$url</a>` 로 그대로 이어붙었다.
 * 이 본문은 **수신자가 나중에 열람하는 값**이고 웹 뷰어도 같은 HTML 을 읽으므로, 저장되는
 * 순간 다른 사람에게 실리는 스크립트가 됐다.
 *
 * 순수 JVM 함수라 Android 없이 돈다.
 */
class BodyLinkHrefTest {
    @Test
    fun `http 와 https 는 그대로 통과한다`() {
        assertEquals("https://example.com/a", "https://example.com/a".toBodyLinkHrefOrNull())
        assertEquals("http://example.com", "http://example.com".toBodyLinkHrefOrNull())
    }

    @Test
    fun `스킴을 안 적으면 https 로 읽는다`() {
        // 시트 placeholder 가 «URL을 입력하세요.» 뿐이라 호스트부터 적는 것이 흔하다.
        assertEquals("https://example.com", "example.com".toBodyLinkHrefOrNull())
        assertEquals("https://example.com/path", "example.com/path".toBodyLinkHrefOrNull())
    }

    @Test
    fun `스킴 대소문자는 무시한다`() {
        assertEquals("https://example.com", "HTTPS://example.com".toBodyLinkHrefOrNull())
    }

    @Test
    fun `javascript 스킴은 거부한다`() {
        // 이 PR 이 막으려는 바로 그것 — 저장되면 열람자 쪽에서 실행되는 값이 된다.
        assertNull("javascript:alert(1)".toBodyLinkHrefOrNull())
        assertNull("JavaScript:alert(1)".toBodyLinkHrefOrNull())
        assertNull("javascript://example.com/%0aalert(1)".toBodyLinkHrefOrNull())
    }

    @Test
    fun `http https 가 아닌 스킴은 https 를 덧붙여 살려 내지 않는다`() {
        assertNull("data:text/html,<script>alert(1)</script>".toBodyLinkHrefOrNull())
        assertNull("file:///etc/passwd".toBodyLinkHrefOrNull())
        assertNull("content://media/external/images/1".toBodyLinkHrefOrNull())
        assertNull("intent://scan/#Intent;scheme=zxing;end".toBodyLinkHrefOrNull())
    }

    @Test
    fun `한글 도메인은 punycode 로 바꿔 통과시킨다`() {
        // 붙여넣기로 흔히 들어오는 정상 주소다 — 거부하면 사용자가 못 넣는다.
        assertEquals("https://xn--bj0bj06e.com", "https://한글.com".toBodyLinkHrefOrNull())
    }

    @Test
    fun `한글 경로는 percent-encoding 해서 통과시킨다`() {
        assertEquals(
            "https://example.com/%ED%95%9C%EA%B8%80",
            "https://example.com/한글".toBodyLinkHrefOrNull(),
        )
    }

    @Test
    fun `속성을 닫으려는 따옴표와 꺾쇠는 통째로 거부한다`() {
        // `"`·`<`·`>` 는 URI 문법에 없는 문자라 파서 단계에서 떨어진다 — 이스케이프까지 갈 것도 없이
        // 아예 안 들어간다. 종전에는 이 입력이 href 를 닫고 새 태그를 열었다.
        assertNull("""https://example.com/?q="><img/src=x/onerror=alert(1)>""".toBodyLinkHrefOrNull())
        assertNull("""https://example.com/?q="><img src=x onerror=alert(1)>""".toBodyLinkHrefOrNull())
    }

    @Test
    fun `URI 로는 정상이지만 HTML 에서 의미를 갖는 문자는 이스케이프한다`() {
        // 쿼리스트링의 `&` 가 대표적이다 — 흔한 정상 주소이고, 안 바꾸면 href 안에서 엔티티 경계가 된다.
        assertEquals(
            "https://example.com/?a=1&amp;b=2",
            "https://example.com/?a=1&b=2".toBodyLinkHrefOrNull(),
        )
    }

    @Test
    fun `홑따옴표는 바꾸지 않는다`() {
        // 큰따옴표 속성 안에서 `'` 는 경계를 못 만든다. 반대로 `&#39;` 로 바꾸면 리치 에디터가
        // 속성값의 숫자 문자 참조를 디코드하지 않아 `&amp;#39;` 로 굳고, **저장되는 주소가 달라진다**
        // (#1067 리뷰). 왕복 뒤 값은 RichTextRoundTripTest 가 본다.
        assertEquals(
            "https://example.com/it's",
            "https://example.com/it's".toBodyLinkHrefOrNull(),
        )
    }

    @Test
    fun `포트만 적은 입력은 스킴 선언으로 오해하지 않는다`() {
        // 스킴 문자 집합에 `.` 이 있어 `example.com` 이 통째로 스킴으로 매치된다 — 「`://` 없고
        // 콜론 뒤가 전부 숫자」면 host:port 로 읽는다 (#1067 리뷰).
        assertEquals("https://example.com:8080/path", "example.com:8080/path".toBodyLinkHrefOrNull())
        assertEquals("https://localhost:3000", "localhost:3000".toBodyLinkHrefOrNull())
        // 콜론 뒤가 숫자가 아니면 여전히 스킴 선언이다.
        assertNull("javascript:alert(1)".toBodyLinkHrefOrNull())
    }

    @Test
    fun `userinfo 위장은 거부한다`() {
        // 보이는 호스트와 실제 목적지가 다른 고전적 위장.
        assertNull("https://google.com@evil.com".toBodyLinkHrefOrNull())
    }

    @Test
    fun `전각 구분자로 우회하는 위장도 거부한다`() {
        // IDN nameprep(NFKC) 이 전각 문자를 ASCII 구분자로 되돌린다 — 원문에 대고만 검사하면
        // `＠` 하나로 뚫린다. 링크 텍스트는 원문을 보여 주므로 눈에는 google.com 으로 읽힌다 (#1067 리뷰).
        assertNull("https://google.com＠evil.com".toBodyLinkHrefOrNull())
        assertNull("google.com＠evil.com".toBodyLinkHrefOrNull())
        assertNull("https://evil.com／path".toBodyLinkHrefOrNull())
        assertNull("https://evil.com：8080".toBodyLinkHrefOrNull())
    }

    @Test
    fun `전각 마침표처럼 호스트로 정상 매핑되는 입력은 통과한다`() {
        // 위 가드가 정상 입력까지 막지 않는지 — 반대 방향.
        assertEquals("https://xn--bj0bj06e.com", "https://한글。com".toBodyLinkHrefOrNull())
    }

    @Test
    fun `공백과 제어문자가 섞이면 거부한다`() {
        assertNull("https://example.com/a b".toBodyLinkHrefOrNull())
        assertNull("java\nscript:alert(1)".toBodyLinkHrefOrNull())
        assertNull("   ".toBodyLinkHrefOrNull())
        assertNull("".toBodyLinkHrefOrNull())
    }

    @Test
    fun `호스트가 없으면 거부한다`() {
        assertNull("https://".toBodyLinkHrefOrNull())
        assertNull("https:///path".toBodyLinkHrefOrNull())
    }

    @Test
    fun `포트는 숫자만 허용한다`() {
        assertEquals("https://example.com:8080/a", "https://example.com:8080/a".toBodyLinkHrefOrNull())
        assertNull("https://example.com:port/a".toBodyLinkHrefOrNull())
    }

    @Test
    fun `이스케이프는 앰퍼샌드를 먼저 바꾼다`() {
        // 순서가 뒤집히면 앞서 만든 엔티티의 & 를 다시 인코딩해 `&amp;lt;` 가 된다.
        assertEquals("&amp;lt;", "&lt;".escapeHtml())
        assertEquals("&lt;a&gt;", "<a>".escapeHtml())
        assertEquals("&quot;", "\"".escapeHtml())
        // `'` 는 큰따옴표 속성 안에서 무해하고, 바꾸면 에디터 왕복에서 깨진다.
        assertEquals("it's", "it's".escapeHtml())
    }
}
