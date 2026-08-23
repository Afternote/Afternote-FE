package com.afternote.feature.mindrecord.presentation.util

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 첨부 파일명 표시 규칙 (#731).
 *
 * 종전에는 링크 텍스트가 `content://com.android.providers.media.documents/document/audio%3A…`
 * 였다. 다른 기기에서 해석되지 않는 로컬 주소인 데다 무엇을 첨부했는지도 알 수 없었다.
 * 어떤 경로로 떨어지든 `content://` 가 본문에 노출되지 않는 것을 고정한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaDisplayNameTest {
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
    fun `대체 이름을 지정할 수 있다`() {
        val uri = Uri.parse("content://com.android.providers.media.documents/document/audio%3A1")

        assertEquals("음성", context.mediaDisplayName(uri, fallback = "음성"))
    }
}
