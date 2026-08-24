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
        .trim()

/**
 * 화면상 **비어 있는** 본문인지 (#722).
 *
 * compose-rich-editor 는 빈 문단도 `<p></p>`·`<br>` 로 직렬화한다. 그래서 직렬화된
 * 문자열에 `isNotBlank()` 를 걸면 화면이 비어 있어도 검증을 통과했고, 빈 데일리질문이
 * 임시저장되면서 작성 화면이 pop 돼 마음의 기록 홈으로 튕겼다.
 *
 * 태그를 벗기고 엔티티를 공백으로 바꾼 뒤 판정한다 — `&nbsp;` 만 실제 공백으로 본다.
 */
fun String.isHtmlBlank(): Boolean =
    replace(HTML_TAG, "")
        .replace(HTML_ENTITY) { match -> if (match.value == "&nbsp;") " " else "" }
        .isBlank()

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

private val HTML_TAG = Regex("<[^>]*>")
private val HTML_ENTITY = Regex("&[a-zA-Z]+;|&#\\d+;")

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
