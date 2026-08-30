package com.afternote.feature.afternote.domain.repository.author

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 추모 음성 지원 형식 판정 (#1118).
 *
 * 이 표가 서버(`S3Service.AUDIO_EXTENSIONS = {mp3, m4a, wav}`)와 어긋나면 두 방향으로 다 샌다 —
 * 좁으면 붙일 수 있는 파일을 막고, 넓으면 업로드는 되는데 저장이 400 으로 끝난다.
 */
class MemorialAudioFormatsTest {
    @Test
    fun `서버가 받는 세 확장자로만 해석된다`() {
        assertEquals("mp3", MemorialAudioFormats.extensionFor("audio/mpeg"))
        assertEquals("m4a", MemorialAudioFormats.extensionFor("audio/mp4"))
        assertEquals("m4a", MemorialAudioFormats.extensionFor("audio/x-m4a"))
        assertEquals("wav", MemorialAudioFormats.extensionFor("audio/x-wav"))
    }

    @Test
    fun `해석 결과는 서버 허용 집합을 벗어나지 않는다`() {
        val allowedByServer = setOf("mp3", "m4a", "wav")

        val resolved = MemorialAudioFormats.supportedMimeTypes.mapNotNull(MemorialAudioFormats::extensionFor)

        assertEquals(MemorialAudioFormats.supportedMimeTypes.size, resolved.size)
        assertTrue(resolved.toSet().all { it in allowedByServer })
    }

    @Test
    fun `대소문자와 charset 파라미터가 붙어도 같은 확장자로 해석된다`() {
        // ContentResolver 가 기기에 따라 "AUDIO/MPEG" · "audio/mpeg; charset=utf-8" 로 돌려준다.
        assertEquals("mp3", MemorialAudioFormats.extensionFor("AUDIO/MPEG"))
        assertEquals("mp3", MemorialAudioFormats.extensionFor("audio/mpeg; charset=utf-8"))
        assertEquals("m4a", MemorialAudioFormats.extensionFor("  audio/x-m4a  "))
    }

    @Test
    fun `서버가 안 받는 녹음 형식은 null 이다`() {
        // 안드로이드 기본 녹음기가 흔히 내놓는 형식들 — 여기서 걸러야 저장 400 이 아니라 첨부 단계 안내가 된다.
        assertNull(MemorialAudioFormats.extensionFor("audio/amr"))
        assertNull(MemorialAudioFormats.extensionFor("audio/3gpp"))
        assertNull(MemorialAudioFormats.extensionFor("audio/ogg"))
        assertNull(MemorialAudioFormats.extensionFor("audio/flac"))
    }

    @Test
    fun `MIME 을 못 읽었거나 음성이 아니면 null 이다`() {
        assertNull(MemorialAudioFormats.extensionFor(null))
        assertNull(MemorialAudioFormats.extensionFor(""))
        assertNull(MemorialAudioFormats.extensionFor("video/mp4"))
        assertNull(MemorialAudioFormats.extensionFor("image/jpeg"))
    }

    @Test
    fun `선택기 필터는 음성 MIME 만 담는다`() {
        assertTrue(MemorialAudioFormats.supportedMimeTypes.isNotEmpty())
        assertTrue(MemorialAudioFormats.supportedMimeTypes.all { it.startsWith("audio/") })
    }
}
