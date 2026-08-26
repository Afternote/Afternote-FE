package com.afternote.feature.mindrecord.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.afternote.feature.mindrecord.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

/**
 * 첨부 파일명 표시 규칙 (#731).
 *
 * 종전에는 링크 텍스트가 `content://com.android.providers.media.documents/document/audio%3A…`
 * 였다. 다른 기기에서 해석되지 않는 로컬 주소인 데다 무엇을 첨부했는지도 알 수 없었다.
 * 어떤 경로로 떨어지든 `content://` 가 본문에 노출되지 않는 것을 고정한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaAttachmentTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `이름을 못 얻으면 마지막 경로 조각을 쓴다`() {
        val uri = Uri.parse("content://com.android.providers.downloads.documents/document/recording.mp3")

        assertEquals("recording.mp3", context.mediaDisplayName(uri))
    }

    @Test
    fun `문서 제공자의 콜론 표기는 파일명으로 쓰지 않는다`() {
        // `document/audio%3A1000000123` 처럼 콜론이 든 조각은 사람이 읽을 이름이 아니다.
        val uri = Uri.parse("content://com.android.providers.media.documents/document/audio%3A1000000123")

        assertEquals("첨부파일", context.mediaDisplayName(uri))
    }

    @Test
    fun `어떤 경로로 떨어져도 content 스킴이 본문에 노출되지 않는다`() {
        val uris =
            listOf(
                "content://com.android.providers.media.documents/document/audio%3A1",
                "content://media/external/audio/media/42",
                "content://com.android.providers.downloads.documents/document/msf%3A88",
            )

        uris.forEach { raw ->
            val name = context.mediaDisplayName(Uri.parse(raw))
            assertFalse("파일명에 로컬 URI 가 새면 안 된다: $name", name.contains("content://"))
        }
    }

    @Test
    fun `대체 이름을 문자열 리소스로 지정할 수 있다`() {
        val uri = Uri.parse("content://com.android.providers.media.documents/document/audio%3A1")

        assertEquals(
            context.getString(R.string.mindrecord_write_media_default_name),
            context.mediaDisplayName(uri, R.string.mindrecord_write_media_default_name),
        )
    }

    /**
     * 본문 이미지 크기는 **원본 비율을 지킨다** (#731 리뷰).
     *
     * 종전에는 320x240 이 상수로 박혀 있어 세로 사진도 저장된 본문에 4:3 으로 들어갔고,
     * 이 값을 존중하는 뷰어(수신자 화면·웹)에서 찌그러져 보였다.
     */
    @Test
    fun `세로 사진은 세로 비율 그대로 계산한다`() {
        val uri = pngUri(width = 100, height = 200)

        assertEquals(320 to 640, context.mediaImageSize(uri, targetWidthPx = 320))
    }

    @Test
    fun `가로 사진은 가로 비율 그대로 계산한다`() {
        val uri = pngUri(width = 400, height = 100)

        assertEquals(320 to 80, context.mediaImageSize(uri, targetWidthPx = 320))
    }

    @Test
    fun `원본 크기를 못 읽으면 비율을 지어내지 않는다`() {
        val notAnImage = File.createTempFile("attachment", ".bin").apply { writeText("not an image") }

        assertEquals(320 to 320, context.mediaImageSize(Uri.fromFile(notAnImage), targetWidthPx = 320))
    }

    /**
     * EXIF 회전은 파일 픽셀과 «보이는» 크기를 갈라 놓는다 (#731 리뷰).
     *
     * 요즘 폰은 세로로 찍어도 센서 방향(가로)으로 저장하고 «90도 돌려서 보라» 를 EXIF 에
     * 남긴다. `BitmapFactory` 는 그걸 무시하므로, 회전을 안 읽으면 400x300 파일이 320x240 —
     * **걷어내려던 상수와 같은 값** — 으로 계산된다. 첨부에서 가장 흔한 입력이 카메라 세로
     * 촬영본이라 여기서 고정한다.
     */
    @Test
    fun `EXIF 90도가 걸린 사진은 돌린 뒤 비율로 계산한다`() {
        val uri = jpegUri(width = 400, height = 300, orientation = ExifInterface.ORIENTATION_ROTATE_90)

        // 보이는 비율은 300:400 이다 — 320 폭이면 427.
        assertEquals(320 to 426, context.mediaImageSize(uri, targetWidthPx = 320))
    }

    @Test
    fun `EXIF 180도는 비율이 그대로다`() {
        // 폭과 높이가 뒤바뀌지 않는 방향까지 맞바꾸면 이번엔 반대로 틀린다.
        val uri = jpegUri(width = 400, height = 300, orientation = ExifInterface.ORIENTATION_ROTATE_180)

        assertEquals(320 to 240, context.mediaImageSize(uri, targetWidthPx = 320))
    }

    @Test
    fun `EXIF 가 없는 사진은 파일 픽셀 그대로 쓴다`() {
        val uri = jpegUri(width = 400, height = 300, orientation = null)

        assertEquals(320 to 240, context.mediaImageSize(uri, targetWidthPx = 320))
    }

    private fun jpegUri(
        width: Int,
        height: Int,
        orientation: Int?,
    ): Uri {
        val file = File.createTempFile("attachment", ".jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        if (orientation != null) {
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
        }
        return Uri.fromFile(file)
    }

    private fun pngUri(
        width: Int,
        height: Int,
    ): Uri {
        val file = File.createTempFile("attachment", ".png")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }
}
