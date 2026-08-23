package com.afternote.feature.mindrecord.presentation.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * 첨부 파일을 사람이 알아볼 이름으로 바꾼다 (`녹음.mp3`, `계약서.pdf`).
 *
 * 종전에는 링크 텍스트로 `content://com.android.providers.media.documents/document/audio%3A…`
 * 를 그대로 썼다. 다른 기기에서 해석되지 않는 로컬 주소인 데다, 무엇을 첨부했는지도
 * 알아볼 수 없었다 (#731).
 *
 * 이름을 못 얻으면 마지막 경로 조각으로, 그것도 없으면 [fallback] 으로 떨어진다 —
 * 어느 경우에도 `content://` 를 본문에 노출하지 않는다.
 */
fun Context.mediaDisplayName(
    uri: Uri,
    fallback: String = "첨부파일",
): String {
    val fromProvider =
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()

    return fromProvider?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() && !it.contains(':') }
        ?: fallback
}
