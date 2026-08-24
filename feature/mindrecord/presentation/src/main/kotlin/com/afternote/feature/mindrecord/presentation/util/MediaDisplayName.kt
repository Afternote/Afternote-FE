package com.afternote.feature.mindrecord.presentation.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import com.afternote.feature.mindrecord.presentation.R

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
    @StringRes fallbackResId: Int = R.string.mindrecord_write_media_default_name,
): String {
    val fromProvider =
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()

    return fromProvider?.takeIf { it.isNotBlank() }
        // lastPathSegment 는 이미 '/' 로 자른 마지막 조각이라 다시 자를 것이 없다.
        ?: uri.lastPathSegment?.takeIf { it.isNotBlank() && !it.contains(':') }
        ?: getString(fallbackResId)
}

/**
 * 본문에 넣을 이미지 표시 크기 — **원본 비율을 지킨다** (#731 리뷰).
 *
 * 크기를 비워 두면 직렬화 때 `width="0"` 이 붙어 어디서도 보이지 않아 값을 넣어야 하는데,
 * 320×240 처럼 박으면 세로 사진이 저장된 본문에 4:3 으로 박제된다 — 이 값을 존중하는
 * 뷰어(수신자 화면·웹)에서 찌그러지고, 이미 저장된 기록은 나중에 고치기도 어렵다.
 *
 * 원본 크기를 못 읽으면 가로 기준값만 쓰고 높이는 비례로 두지 않는다 — 알 수 없는 비율을
 * 지어내지 않기 위해 정사각으로 떨어뜨린다.
 */
fun Context.mediaImageSize(
    uri: Uri,
    targetWidthPx: Int,
): Pair<Int, Int> {
    val bounds =
        runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                options
            }
        }.getOrNull()

    val width = bounds?.outWidth ?: 0
    val height = bounds?.outHeight ?: 0
    if (width <= 0 || height <= 0) return targetWidthPx to targetWidthPx

    return targetWidthPx to (targetWidthPx.toLong() * height / width).toInt().coerceAtLeast(1)
}
