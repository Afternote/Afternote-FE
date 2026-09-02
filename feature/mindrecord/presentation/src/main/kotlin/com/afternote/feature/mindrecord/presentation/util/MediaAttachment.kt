package com.afternote.feature.mindrecord.presentation.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import androidx.exifinterface.media.ExifInterface
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

    val fileWidth = bounds?.outWidth ?: 0
    val fileHeight = bounds?.outHeight ?: 0
    if (fileWidth <= 0 || fileHeight <= 0) return targetWidthPx to targetWidthPx

    // 파일 픽셀과 «보이는» 크기가 다를 수 있다 — 아래 참조.
    val (width, height) =
        if (isUriRotatedQuarterTurn(uri)) fileHeight to fileWidth else fileWidth to fileHeight

    return targetWidthPx to (targetWidthPx.toLong() * height / width).toInt().coerceAtLeast(1)
}

/**
 * 이 이미지를 뷰어가 **90도 돌려서** 보는가 (= 폭과 높이가 뒤바뀌는가).
 *
 * `BitmapFactory` 의 `outWidth`/`outHeight` 는 파일에 적힌 픽셀 크기라 EXIF `Orientation` 을
 * 반영하지 않는다. 요즘 폰은 세로로 찍어도 센서 방향(가로)으로 저장하고 «돌려서 보라» 를
 * EXIF 에 남기는데, 갤러리·웹뷰어는 그걸 존중하고 `BitmapFactory` 는 무시한다.
 *
 * 그래서 이걸 빼면 «400x300 파일 + Orientation=6» 이 320×240 으로 계산돼 **종전 상수와 같은
 * 값**이 된다 — 비율 계산으로 바꾼 의미가 카메라 세로 촬영본에서만 통째로 사라진다. 첨부
 * 입력에서 가장 흔한 쪽이라 여기서 잡는다 (#731 리뷰).
 *
 * 회전을 못 읽으면 «안 돌아간 것» 으로 본다 — 파일 픽셀 그대로 쓰는 종전 동작이다.
 * (`ImageDecoder` 는 헤더 크기에 EXIF 를 적용해 준다. minSdk 28 이 되어 이제 쓸 수 있으므로
 * 이 수동 보정을 걷어낼 수 있다 — 별도 과제로 남긴다.)
 */
private fun Context.isUriRotatedQuarterTurn(uri: Uri): Boolean {
    val orientation =
        runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    return orientation in QUARTER_TURN_ORIENTATIONS
}

/** 폭과 높이가 뒤바뀌는 방향들. 180도 회전·좌우 반전은 비율이 그대로라 여기 없다. */
private val QUARTER_TURN_ORIENTATIONS =
    setOf(
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE,
    )
