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
