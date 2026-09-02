package com.afternote.feature.afternote.presentation.editor.memorial

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * 즉석 촬영 결과 파일과 그 `content://` URI 계약 (#369).
 *
 * 여기서 지키는 것은 매니페스트(`FileProvider` authority)·`afternote_file_paths.xml`(cache-path)·
 * 코드 상수 셋이 서로 어긋나지 않는다는 것이다. 어긋나면 `IllegalArgumentException` 으로 터지는데
 * 그 지점이 카메라를 띄우려는 순간이라, 조립 오류치고는 드러나는 시점이 늦다.
 *
 * SDK 를 못 박는 이유: 라이브러리 모듈은 targetSdk 를 따로 두지 않아 compileSdk(37) 가 그대로 실리는데
 * Robolectric 4.15 가 아는 최신 SDK 는 35 라, 두면 "targetSdkVersion > maxSdkVersion" 으로 기동부터
 * 실패한다. 여기서 보는 것은 조립이지 플랫폼 버전별 동작이 아니라 고정으로 충분하다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MemorialMediaCaptureFilesTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    /**
     * 한 메서드에 다 넣은 것은 의도다. `FileProvider` 는 authority 하나당 경로 전략을 **static** 으로
     * 캐시하는데, Robolectric 은 테스트 *메서드마다* 다른 임시 dataDir 을 준다. 메서드를 나누면 두 번째
     * 메서드부터 첫 메서드의 캐시(그 메서드의 임시 경로)를 물려받아 "루트를 못 찾는다" 로 실패한다.
     * 프로덕션에는 없는 제약이라 코드를 비트는 대신 테스트를 한 흐름으로 둔다.
     */
    @Test
    fun `촬영 결과는 매니페스트 authority 로 노출되고 확장자가 MIME 으로 역산된다`() {
        val photo = createMemorialCaptureUri(context, "jpg")

        // (1) 매니페스트의 android:authorities 와 코드 상수가 같은 문자열인가.
        assertEquals("content", photo.scheme)
        assertEquals(context.packageName + ".afternote.fileprovider", photo.authority)

        // (2) 업로드는 ContentResolver.getType() 으로 presigned 확장자를 정한다. 여기가 octet-stream 이면
        //     촬영본이 전부 기본 확장자(jpg)로 올라간다 — 영상이 .jpg 로 올라가는 형태.
        val video = createMemorialCaptureUri(context, "mp4")
        assertEquals("image/jpeg", context.contentResolver.getType(photo))
        assertEquals("video/mp4", context.contentResolver.getType(video))

        // (3) 파일은 file_paths.xml 이 노출하는 캐시 하위 디렉터리에 있고, 호출마다 새로 만들어진다
        //     (재사용하면 두 번째 촬영이 첫 결과를 덮어쓴다).
        val dir = File(context.cacheDir, "captured_media")
        assertTrue(dir.isDirectory)
        assertNotEquals(photo, video)
        assertEquals(2, dir.listFiles().orEmpty().size)

        // (4) 취소 시 폐기하면 0바이트 파일이 남지 않는다.
        //     "지울 것이 없는" 경우(pending 이 비어 있음)는 호출부가 거르므로 여기 갈래가 없다 — non-null 파라미터.
        discardMemorialCapture(context, video)
        assertEquals(listOf("jpg"), dir.listFiles().orEmpty().map { it.extension })
    }
}
