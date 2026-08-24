package com.afternote.feature.mindrecord.presentation.util

import androidx.core.text.HtmlCompat
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
 * 업로드 직후 받은 파일 URL 을 **서버가 본문에서 기대하는 형태**(fileKey) 로 바꾼다.
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
 * 스킴과 호스트만 떼면 fileKey 가 되므로 경로 규칙(`mindrecords/staging/...`)을 코드에
 * 박지 않는다 — 서버가 디렉터리 구조를 바꿔도 따라간다.
 *
 * 다만 이 방식은 `fileUrl == "<스킴>://<호스트>/" + fileKey` 를 가정한다. **권위 있는
 * 출처는 presigned 응답의 `fileKey` 인데 `PhotoUploadRepository.upload()` 가 `fileUrl` 만
 * 돌려주며 버린다** — CDN 이 경로 프리픽스나 쿼리스트링을 붙이면 조용히 틀린 키가 된다.
 * 반환을 넓히는 것은 `core:data` 범위라 별건으로 둔다 (#1017).
 */
fun String.toUploadedFileKey(): String =
    // 스킴이 없으면 substringAfter("://") 가 원문을 그대로 돌려주고, 이어지는
    // substringAfter('/') 가 첫 경로 세그먼트를 잘라먹는다 — 조용히 틀린 키가 나간다.
    if (contains("://")) substringAfter("://").substringAfter('/') else this

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
 * 본문 링크로 쓸 수 있는 URL 인지 (#722).
 *
 * 종전에는 임의 문자열도 그대로 `href` 가 됐다. 스킴을 http/https 로 제한하고 호스트가
 * 있는지까지 본다 — `javascript:` 같은 스킴이 본문에 들어갈 자리를 없앤다.
 */
fun String.isSupportedLinkUrl(): Boolean {
    val trimmed = trim()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return false
    val parsed = runCatching { URI(trimmed) }.getOrNull() ?: return false
    val scheme = parsed.scheme?.lowercase() ?: return false
    if (scheme != "http" && scheme != "https") return false
    return !parsed.host.isNullOrBlank()
}

/**
 * HTML 속성·본문에 넣기 전 이스케이프 (#722).
 *
 * 검증을 통과한 URL 이라도 따옴표나 꺾쇠가 섞이면 `a` 태그를 깨고 나올 수 있다.
 */
fun String.escapeHtml(): String =
    buildString(length) {
        this@escapeHtml.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }

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
