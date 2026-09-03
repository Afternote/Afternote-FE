package com.afternote.feature.afternote.presentation.editor.memorial

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * 촬영 결과가 떨어질 캐시 하위 디렉터리. `afternote_file_paths.xml` 의 `<cache-path path="captured_media/">`
 * 와 같은 값이어야 [FileProvider] 가 URI 를 만들어 준다 — 다르면 `IllegalArgumentException` 으로 터진다.
 */
private const val CAPTURE_DIR = "captured_media"

/** 매니페스트의 `android:authorities="${applicationId}.afternote.fileprovider"` 와 짝. */
private const val AUTHORITY_SUFFIX = ".afternote.fileprovider"

/**
 * 카메라 앱이 결과를 써 넣을 빈 파일을 캐시에 만들고, 그 파일을 가리키는 `content://` URI 를 돌려준다.
 *
 * 카메라 앱은 다른 프로세스라 `file://` 경로를 넘기면 Android 7+ 에서 `FileUriExposedException` 이 난다.
 * [FileProvider] 로 감싸 URI 하나에만 쓰기 권한을 위임한다(`grantUriPermission`).
 *
 * 확장자를 파일명에 박아 두는 이유: 업로드 경로가 `ContentResolver.getType()` 으로 MIME 을 정하는데,
 * [FileProvider] 는 그 MIME 을 파일 확장자에서 역산한다. 확장자가 없으면 `application/octet-stream` 이
 * 되어 presigned 발급 확장자가 기본값으로 떨어진다.
 */
internal fun createMemorialCaptureUri(
    context: Context,
    extension: String,
): Uri {
    val dir = File(context.cacheDir, CAPTURE_DIR)
    dir.mkdirs()
    val file = File.createTempFile("capture_", ".$extension", dir)
    return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
}

/**
 * 촬영이 취소·실패해 쓰이지 않은 빈 파일을 지운다.
 *
 * 캐시라 방치해도 OS 가 언젠가 회수하지만, 취소를 반복하면 0바이트 파일이 계속 쌓인다.
 * 삭제 실패는 무시한다 — 지우지 못해도 사용자에게 알릴 것이 없다.
 *
 * "지울 것이 없다" 는 호출부가 아는 사실이라 여기서 받지 않는다 — [uri] 가 non-null 인 덕에
 * 이 함수는 "지운다" 하나만 한다.
 */
internal fun discardMemorialCapture(
    context: Context,
    uri: Uri,
) {
    runCatching { context.contentResolver.delete(uri, null, null) }
}
