package com.afternote.feature.mindrecord.presentation.util

import androidx.core.text.HtmlCompat
import java.net.IDN
import java.net.URI

/**
 * HTML 직렬화된 본문에서 태그를 제거해 리스트 카드에 노출할 plain text 로 변환.
 *
 * 작성 화면(WriteTextField → compose-richeditor)이 `<a href>` / `<img>` / `<b>` 등의 태그로
 * 본문을 직렬화하기 때문에, 카드 미리보기에선 태그를 제거해야 한다. 원본 HTML 은 도메인/
 * ViewModel 상태에 그대로 유지되며, 본 함수는 표시 시점에만 변환한다.
 */
fun String.htmlToPlainText(): String =
    HtmlCompat
        .fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString()
        // ImageGetter 없이 파싱하면 img 자리에 U+FFFC 가 남는다. 본문에 img 를 정식으로
        // 넣기 시작하면(#549) 카드 미리보기 둘째 줄이 통째로 `￼` 가 된다 — 공백이 아니라
        // trim() 으로는 안 지워진다.
        .replace("\uFFFC", "")
        .trim()

/**
 * 본문 HTML 의 **첫 `img[src]`** 를 목록 카드 썸네일로 뽑는다 (없으면 null).
 *
 * 데일리질문·일기의 요청/응답 계약 어디에도 `imageUrl` 필드가 없다 (#549). 서버 설계는
 * 본문 이미지를 `content` HTML 의 `img` 태그로 담는 것이고, `content` 설명이 허용 태그로
 * `img[src|alt|width|height|style]` 을 명시한다. 실서버에서도 `<img src>` 는 저장·재조회에
 * 그대로 살아남는다 (2026-08-23 실측).
 *
 * 그래서 썸네일의 출처는 별도 필드가 아니라 본문이다. 정규식으로 훑는 이유는 이 값이
 * **표시용 한 장**이라, 파서를 들이기보다 실패해도 썸네일이 안 뜨는 정도로 끝나는 편이
 * 낫기 때문이다 — 본문 렌더링은 여전히 에디터/`htmlToPlainText` 가 맡는다.
 */
fun String.firstHtmlImageSrcOrNull(): String? =
    HTML_IMG_SRC
        .find(this)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

// `src` 가 img 태그의 몇 번째 속성이든 잡히도록 태그 안을 훑는다. 따옴표는 " 와 ' 둘 다 허용.
private val HTML_IMG_SRC = Regex("""<img\b[^>]*?\bsrc\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)

/**
 * 제출 직전, **이번 작성 중 업로드한** 이미지의 `src` 를 서버가 기대하는 fileKey 로 바꾼다
 * (#549 · #1016 · #1125).
 *
 * `POST /files/presigned-url` 은 `fileKey`(`mindrecords/staging/13/uuid.png`) 와
 * `fileUrl`(그 앞에 CDN 호스트가 붙은 전체 URL) 을 함께 준다. 저장 시 서버는 본문의
 * `img src` 를 훑어 **staging 파일을 permanent 로 옮기고 전체 URL 로 재작성**하는데,
 * 이때 기대하는 입력이 fileKey 다. 전체 URL 을 그대로 넣으면 그 앞에 호스트를 한 번 더
 * 붙여 버린다 (실서버 실측 2026-08-23, #549).
 *
 * ```
 * 보낸 값: https://cdn.example.net/mindrecords/staging/13/a.png
 * 저장된 값: https://cdn.example.net/https://cdn.example.net/mindrecords/permanent/13/a.png  → 403
 * ```
 *
 * **서버가 준 키를 그대로 쓴다.** 종전에는 URL 에서 스킴·호스트를 떼어 키를 역산했는데,
 * 그건 `upload()` 가 `fileUrl` 만 돌려주던 시절의 우회였다. `fileUrl == "<스킴>://<호스트>/" + fileKey`
 * 를 가정하므로 CDN 이 경로 프리픽스를 붙이거나 쿼리스트링이 붙으면 **조용히 틀린 키가 나가고**,
 * 증상은 #549 가 고친 「저장은 됐는데 이미지가 안 뜬다」와 같다. #1017 이 `upload()` 반환을
 * `UploadedFile(fileUrl, fileKey)` 로 넓혀 그 가정이 필요 없어졌다 (#1125).
 *
 * 이미 저장돼 본문에 들어 있는 영구 URL 은 건드리지 않는다 — 서버가 그대로 통과시키고,
 * 키로 바꾸면 이미 옮겨진 파일을 다시 옮기려다 실패한다. 그래서 경로 패턴으로 훑지 않고
 * 이번에 받은 URL 만 정확히 치환한다.
 *
 * 데일리질문과 일기가 같은 규칙을 쓴다 — 한쪽에만 있어서 일기 본문 이미지가 깨졌다 (#1016).
 *
 * @param uploadedFileKeysByUrl 이번 작성에서 업로드한 `fileUrl` → 서버가 준 `fileKey` 대응.
 */
fun String.toWireContent(uploadedFileKeysByUrl: Map<String, String>): String {
    if (uploadedFileKeysByUrl.isEmpty()) return this
    return MEDIA_REFERENCE.replace(this) { match ->
        val (attribute, quote, rawValue) = match.destructured
        // **속성값을 되돌린 뒤 맞춘다.** 리치 에디터가 직렬화하면서 `&` 를 `&amp;` 로 바꾸므로,
        // 원문 URL 로 그대로 찾으면 쿼리가 둘 이상인 주소(`?x=1&y=2`)를 놓친다 — 그러면 전체 URL 이
        // 그대로 서버로 나가 #549 의 이중 호스트·403 이 재발한다 (#1125 리뷰, 실측 확인).
        val fileKey = uploadedFileKeysByUrl[rawValue.unescapeHtmlAttribute()]
        if (fileKey == null) match.value else "$attribute=$quote$fileKey$quote"
    }
}

/** `src`·`href` 속성 하나. 따옴표는 " 와 ' 둘 다 받는다. */
private val MEDIA_REFERENCE = Regex("""\b(src|href)\s*=\s*(["'])(.*?)\2""", RegexOption.IGNORE_CASE)

/**
 * 속성값에 들어간 엔티티를 원문으로 되돌린다.
 *
 * [escapeHtml] 의 정확한 역은 아니다 — 그쪽은 `'` 를 일부러 건드리지 않는데(richeditor 가
 * `&#39;` 를 `&amp;#39;` 로 굳혀 버려서다) 여기서는 `&#39;` 도 되돌린다. 리치 에디터가 내는
 * 형태를 받아내는 쪽이라 넓게 잡아 둔다.
 *
 * `&amp;` 를 **마지막에** 되돌린다. 먼저 되돌리면 `&amp;lt;` 가 `&lt;` 를 거쳐 `<` 로 두 번
 * 풀린다.
 */
private fun String.unescapeHtmlAttribute(): String =
    replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")

/**
 * 태그만 있고 보이는 글자가 없으면 비었다고 본다.
 *
 * 리치 에디터는 아무것도 입력하지 않아도 `<p></p>` 를 내보내므로 `isBlank()` 로는 "화면이
 * 비었는지" 를 판정할 수 없다. 그 값을 "사용자가 이미 썼다" 로 오해하면 이어쓸 임시저장
 * 본문이 실리지 않고, 그대로 저장할 때 기존 내용이 덮인다 (#923).
 *
 * [htmlToPlainText] 와 달리 Android 에 의존하지 않는다 — ViewModel 판정에 쓰이므로 순수
 * JVM 단위 테스트로 돌아야 한다.
 */
fun String.isHtmlBlank(): Boolean {
    // 태그를 통째로 걷으면 이미지·링크처럼 **태그 자체가 내용인** 본문이 빈 것으로 접힌다.
    // 사진만 첨부한 상태에서 이어쓰기가 도착하면 그 이미지가 draft 로 덮인다 (리뷰 지적).
    if (HTML_MEDIA_TAG.containsMatchIn(this)) return false

    return replace(HTML_TAG, "")
        // 공백 엔티티만 공백으로, 나머지 엔티티는 **가시 문자 한 자**로 친다. 종전에는
        // 전부 지워 `<p>&lt;</p>` 같은 본문이 빈 것으로 판정됐다.
        .replace(HTML_ENTITY) { match -> if (match.value in HTML_SPACE_ENTITIES) " " else "\uFFFD" }
        .isBlank()
}

private val HTML_TAG = Regex("<[^>]*>")
private val HTML_ENTITY = Regex("&[a-zA-Z]+;|&#\\d+;")

/** 태그 자체가 내용인 것들 — 걷어내면 «비었다» 로 뒤집힌다. */
private val HTML_MEDIA_TAG = Regex("""<(img|video|audio|iframe|embed)\b""", RegexOption.IGNORE_CASE)

/** 공백으로 렌더되는 엔티티. 나머지는 가시 문자로 본다. */
private val HTML_SPACE_ENTITIES = setOf("&nbsp;", "&#160;", "&ensp;", "&emsp;", "&thinsp;")

/**
 * 사용자가 입력한 링크를 본문 `<a href>` 에 넣을 수 있는 형태로 정규화한다. 넣을 수 없으면 null.
 *
 * 종전에는 **검증이 아예 없었다** — 입력 문자열이 `<a href="$url">$url</a>` 로 그대로
 * 이어붙여져 두 가지가 열려 있었다 (#1067).
 *
 * 1. **스킴 제한 없음.** `javascript:alert(1)` 이 그대로 `href` 에 저장됐다. 이 본문은 수신자가
 *    나중에 열람하는 값이고 웹 뷰어도 같은 HTML 을 읽으므로, 저장되는 순간 다른 사람에게 실리는
 *    스크립트가 된다.
 * 2. **이스케이프 없음.** 따옴표 하나로 속성을 닫고 태그를 덧붙일 수 있었다.
 *
 * 그래서 이 함수는 **넣어도 되는 것만 통과시키고, 통과한 값도 이스케이프해서** 돌려준다.
 *
 * 유니코드는 거부하지 않는다 — 한글 도메인·한글 경로는 붙여넣기로 흔히 들어오는 정상 주소다.
 * 호스트는 [java.net.IDN] 으로 punycode 로 바꾸고, 그 뒤 경로·쿼리는 UTF-8 percent-encoding 한다.
 * `java.net.URI` 는 비ASCII 에서 그냥 던지므로 **변환을 먼저 하고 그다음에 검증**한다.
 *
 * 스킴을 안 적으면 `https` 로 읽는다 — 시트 placeholder 가 «URL을 입력하세요.» 뿐이라 사용자가
 * 호스트부터 적는 것이 흔하다. 다만 **스킴을 적었다면 그 값을 존중**해서, `javascript:` 앞에
 * `https://` 를 붙여 «정상 주소» 로 만들어 버리지 않는다.
 *
 * `user@host` 형태(userinfo)는 거부한다 — `https://google.com@evil.com` 은 보이는 것과 실제 목적지가
 * 다른 고전적인 위장이고, 이 시트로 그걸 만들 이유가 없다.
 */
fun String.toBodyLinkHrefOrNull(): String? {
    val trimmed = trim()
    // 공백·제어문자가 섞이면 속성 경계를 넘거나 사람이 읽는 주소와 실제가 갈린다.
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() || it.isISOControl() }) return null

    // `example.com:8080` 은 스킴 선언이 아니라 host:port 다 — 스킴 문자 집합에 `.` 이 있어
    // `example.com` 이 통째로 스킴으로 매치된다. 「`://` 가 없고 콜론 뒤가 전부 숫자」면 포트로 읽는다.
    // `javascript:alert(1)` 은 콜론 뒤가 숫자가 아니라 그대로 스킴 선언으로 남는다 (#1067 리뷰).
    val declared =
        DECLARED_SCHEME
            .find(trimmed)
            ?.groupValues
            ?.get(1)
            ?.takeUnless { trimmed.looksLikeHostPort(it) }
    val scheme = declared?.lowercase() ?: DEFAULT_SCHEME
    // 스킴을 적었는데 허용 목록 밖이면 거부한다 — https 를 덧붙여 살려 내지 않는다.
    if (scheme !in ALLOWED_SCHEMES) return null

    val rest =
        when (declared) {
            // `javascript:alert(1)` 처럼 `//` 없는 형태는 여기서 걸린다.
            null -> {
                trimmed
            }

            else -> {
                val prefix = "$declared://"
                if (!trimmed.startsWith(prefix, ignoreCase = true)) return null
                trimmed.substring(prefix.length)
            }
        }
    if (rest.isEmpty()) return null

    val authorityEnd = rest.indexOfFirst { it in "/?#" }.takeIf { it >= 0 } ?: rest.length
    val authority = rest.substring(0, authorityEnd)
    val tail = rest.substring(authorityEnd)
    // userinfo 위장 차단. 대괄호(IPv6) 도 이 시트의 입력으로 볼 이유가 없다.
    if (authority.isEmpty() || '@' in authority || '[' in authority || ']' in authority) return null

    val host = authority.substringBefore(':')
    val port = authority.substringAfter(':', missingDelimiterValue = "")
    if (host.isEmpty()) return null
    if (port.isNotEmpty() && (port.toIntOrNull() == null)) return null

    val asciiHost = runCatching { IDN.toASCII(host, IDN.ALLOW_UNASSIGNED) }.getOrNull() ?: return null
    if (asciiHost.isEmpty()) return null
    // **변환한 값에 대고 한 번 더 본다.** nameprep(NFKC) 이 전각 문자를 ASCII 구분자로 되돌리므로,
    // 원문에 대고 한 검사만으로는 `google.com＠evil.com` 이 `google.com@evil.com` 이 되어 통과한다.
    // 그러면 링크 텍스트는 원문을 보여 주고 목적지는 evil.com 이 된다 — 이 함수가 막으려던 위장 그대로다.
    // 정상 호스트에는 이 문자들이 들어갈 수 없다.
    if (asciiHost.any { it in HOST_FORBIDDEN }) return null

    val normalized =
        buildString {
            append(scheme)
            append("://")
            append(asciiHost)
            if (port.isNotEmpty()) append(':').append(port)
            append(tail.percentEncodeNonAscii())
        }

    // 남은 형태 오류(잘못된 percent-encoding 등)는 URI 파서에 맡긴다. 여기까지 왔으면 전부 ASCII 다.
    val parsed = runCatching { URI(normalized) }.getOrNull() ?: return null
    if (parsed.host.isNullOrEmpty()) return null

    return normalized.escapeHtml()
}

/**
 * `&`·`<`·`>`·`"` 를 엔티티로 바꾼다 — 큰따옴표 속성과 텍스트 양쪽에 안전한 최소 집합.
 *
 * `&` 를 **가장 먼저** 바꾼다. 나중에 바꾸면 앞서 만든 엔티티의 `&` 를 다시 인코딩해 `&amp;lt;` 가 된다.
 *
 * **`'` 는 일부러 바꾸지 않는다.** 큰따옴표 속성 안에서 홑따옴표는 경계를 못 만들고, 리치 에디터가
 * 속성값의 숫자 문자 참조를 디코드하지 않아 `&#39;` 를 넣으면 `&amp;#39;` 로 굳어 **저장되는 주소가
 * 달라진다** — 사용자가 적지 않은 값이 서버로 올라간다 (#1067 리뷰). 텍스트 노드 쪽도 `'` 를 그대로
 * 두는 편이 왕복 뒤 원문과 같다.
 */
fun String.escapeHtml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

/** 비ASCII 바이트만 UTF-8 percent-encoding 한다. 이미 인코딩된 `%XX` 는 ASCII 라 그대로 지나간다. */
private fun String.percentEncodeNonAscii(): String =
    buildString {
        for (byte in this@percentEncodeNonAscii.toByteArray(Charsets.UTF_8)) {
            val value = byte.toInt() and 0xFF
            if (value < 0x80) append(value.toChar()) else append('%').append(HEX[value ushr 4]).append(HEX[value and 0xF])
        }
    }

private const val HEX = "0123456789ABCDEF"

/** 입력이 스스로 선언한 스킴. `://` 가 아니라 `:` 까지만 본다 — `javascript:` 도 «선언했다» 로 잡는다. */
private val DECLARED_SCHEME = Regex("""^([A-Za-z][A-Za-z0-9+.\-]*):""")

/**
 * `<scheme>:` 로 매치됐지만 실제로는 `host:port` 인가.
 *
 * `://` 가 없고 콜론 뒤 첫 조각이 전부 숫자일 때만 그렇게 읽는다.
 */
private fun String.looksLikeHostPort(matchedScheme: String): Boolean {
    if (startsWith("$matchedScheme://", ignoreCase = true)) return false
    val afterColon = substring(matchedScheme.length + 1)
    val port = afterColon.takeWhile { it !in "/?#" }
    return port.isNotEmpty() && port.all { it.isDigit() }
}

/** 정상 호스트에는 들어갈 수 없는 구분자들. nameprep 이 전각에서 되돌려 놓는 것들이다. */
private const val HOST_FORBIDDEN = "@/:?#[]\\"

private val ALLOWED_SCHEMES = setOf("http", "https")

private const val DEFAULT_SCHEME = "https"

/**
 * 상세 화면 본문 블록 (#759).
 *
 * 시안은 본문을 «문단 → 이미지 → 문단» 처럼 **섞어서** 보여준다. 본문은 HTML 조각이고
 * 이미지는 그 안의 `img` 태그이므로, 태그를 기준으로 잘라 순서대로 그린다.
 */
sealed interface RecordContentBlock {
    data class Text(
        val text: String,
    ) : RecordContentBlock

    data class Image(
        val url: String,
    ) : RecordContentBlock
}

/**
 * 본문 HTML 을 [RecordContentBlock] 목록으로 자른다.
 *
 * 리치 에디터가 `<img>` 를 렌더하지 못해(#731) 에디터를 그대로 재사용할 수 없다. 대신
 * 이미지 태그를 경계로 잘라 텍스트는 태그를 벗겨 그리고 이미지는 따로 그린다.
 *
 * 빈 텍스트 조각은 버린다 — 이미지 앞뒤의 빈 문단이 그대로 빈 줄이 되면 안 된다.
 */
fun String.toRecordContentBlocks(): List<RecordContentBlock> {
    val blocks = mutableListOf<RecordContentBlock>()
    var cursor = 0
    HTML_IMG_TAG.findAll(this).forEach { match ->
        appendTextBlock(blocks, substring(cursor, match.range.first))
        match.groupValues[1]
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let { blocks += RecordContentBlock.Image(it) }
        cursor = match.range.last + 1
    }
    appendTextBlock(blocks, substring(cursor))
    return blocks
}

private fun appendTextBlock(
    blocks: MutableList<RecordContentBlock>,
    rawHtml: String,
) {
    if (rawHtml.isHtmlBlank()) return
    val text = rawHtml.htmlToPlainText()
    if (text.isNotBlank()) blocks += RecordContentBlock.Text(text)
}

private val HTML_IMG_TAG = Regex("""<img\b[^>]*?\bsrc\s*=\s*["']([^"']*)["'][^>]*>""", RegexOption.IGNORE_CASE)
