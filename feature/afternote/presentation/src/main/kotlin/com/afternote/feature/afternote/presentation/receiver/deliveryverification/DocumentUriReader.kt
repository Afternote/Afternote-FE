package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Immutable

/**
 * 서류 업로드 화면(6·7·8) 에서 Picker 가 돌려준 [Uri] 를 ViewModel 이 다룰 수 있는 ByteArray·메타로 풀어내는 헬퍼.
 *
 * ViewModel/Domain 레이어를 Android Uri/ContentResolver 의존성에서 떼어내기 위해 UI 레이어에 두며,
 * UI Composable 이 LocalContext 의 ContentResolver 로 호출한다.
 */
@Immutable
data class DocumentReadResult(
    val bytes: ByteArray,
    val extension: String,
    val displayName: String,
) {
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * [Uri] 에서 바이트·확장자·표시 이름을 추출. 추출 실패 시 null 반환.
 */
fun ContentResolver.readDocumentUri(uri: Uri): DocumentReadResult? {
    val bytes =
        runCatching {
            openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
    if (bytes.isEmpty()) return null

    val mimeType = getType(uri)
    val extension =
        mimeType
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: uri.lastPathSegment
                ?.substringAfterLast('.', missingDelimiterValue = "")
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_EXTENSION

    val displayName =
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            ?: uri.lastPathSegment.orEmpty()

    return DocumentReadResult(
        bytes = bytes,
        extension = extension,
        displayName = displayName,
    )
}

private const val DEFAULT_EXTENSION = "bin"
