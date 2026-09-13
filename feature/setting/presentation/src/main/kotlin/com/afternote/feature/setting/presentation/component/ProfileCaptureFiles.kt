package com.afternote.feature.setting.presentation.component

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * 촬영 결과가 떨어질 캐시 하위 디렉터리. `setting_file_paths.xml` 의 `<cache-path path="profile_capture/">`
 * 와 같은 값이어야 [FileProvider] 가 URI 를 만들어 준다 — 다르면 `IllegalArgumentException` 으로 터진다.
 *
 * 캐시 루트가 아니라 하위 디렉터리인 것이 요점이다. 루트를 열면 이 provider 로 앱 캐시의 아무 파일이나
 * 건네줄 수 있게 된다.
 */
private const val CAPTURE_DIR = "profile_capture"

/** 매니페스트의 `android:authorities="${applicationId}.setting.fileprovider"` 와 짝. */
private const val AUTHORITY_SUFFIX = ".setting.fileprovider"

/**
 * 프로필 사진은 JPEG 하나뿐이다. 업로드가 `ContentResolver.getType()` 으로 MIME 을 정하고
 * [FileProvider] 는 그 MIME 을 확장자에서 역산하므로, 확장자를 파일명에 박아 둔다 — 없으면
 * `application/octet-stream` 이 되어 presigned 발급 확장자가 기본값으로 떨어진다.
 */
private const val CAPTURE_EXTENSION = "jpg"

/**
 * 카메라 앱이 결과를 써 넣을 빈 파일을 캐시에 만들고, 그 파일을 가리키는 `content://` URI 를 돌려준다.
 *
 * 카메라 앱은 다른 프로세스라 `file://` 경로를 넘기면 Android 7+ 에서 `FileUriExposedException` 이 난다.
 * [FileProvider] 로 감싸 URI 하나에만 쓰기 권한을 위임한다(`grantUriPermission`).
 *
 * 호출마다 새 파일을 만든다 — 재사용하면 두 번째 촬영이 첫 결과를 덮어쓴다.
 *
 * @throws java.io.IOException 캐시에 파일을 만들지 못했을 때(저장공간). 호출부가 안내로 바꾼다.
 */
internal fun createProfileCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, CAPTURE_DIR)
    dir.mkdirs()
    val file = File.createTempFile("profile_capture_", ".$CAPTURE_EXTENSION", dir)
    return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
}

/**
 * 촬영이 취소·실패해 쓰이지 않은 빈 파일을 지운다.
 *
 * 우리가 만든 pending 파일 하나만 지운다 — 디렉터리를 비우지 않는다. 캐시라 방치해도 OS 가 언젠가
 * 회수하지만, 취소를 반복하면 0바이트 파일이 계속 쌓인다. 삭제 실패는 무시한다 — 지우지 못해도
 * 사용자에게 알릴 것이 없다.
 */
internal fun discardProfileCapture(
    context: Context,
    uri: Uri,
) {
    runCatching { context.contentResolver.delete(uri, null, null) }
}
