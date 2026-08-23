package com.afternote.feature.mindrecord.presentation.util

import androidx.core.text.HtmlCompat

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
 * 태그만 있고 보이는 글자가 없으면 비었다고 본다.
 *
 * 리치 에디터는 아무것도 입력하지 않아도 `<p></p>` 를 내보내므로 `isBlank()` 로는 "화면이
 * 비었는지" 를 판정할 수 없다. 그 값을 "사용자가 이미 썼다" 로 오해하면 이어쓸 임시저장
 * 본문이 실리지 않고, 그대로 저장할 때 기존 내용이 덮인다 (#923).
 *
 * [htmlToPlainText] 와 달리 Android 에 의존하지 않는다 — ViewModel 판정에 쓰이므로 순수
 * JVM 단위 테스트로 돌아야 한다.
 */
fun String.isHtmlBlank(): Boolean =
    replace(HTML_TAG, "")
        .replace(HTML_ENTITY) { match -> if (match.value == "&nbsp;") " " else "" }
        .isBlank()

private val HTML_TAG = Regex("<[^>]*>")
private val HTML_ENTITY = Regex("&[a-zA-Z]+;|&#\\d+;")
