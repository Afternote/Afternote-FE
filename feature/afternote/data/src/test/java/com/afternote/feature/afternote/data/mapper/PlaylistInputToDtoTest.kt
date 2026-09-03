package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistInputToDtoTest {
    @Test
    fun `곡 입력은 서버 계약의 제목 아티스트 커버만 wire로 변환한다`() {
        val dtoSong =
            MemorialSongPayload(
                title = "노래",
                artist = "가수",
                coverUrl = "cover",
            ).toDto()

        assertEquals("노래", dtoSong.title)
        assertEquals("가수", dtoSong.artist)
        assertEquals("cover", dtoSong.coverUrl)
    }

    @Test
    fun `추모 음성은 서버 계약 키 memorialAudioUrl 로 실린다`() {
        // 키 이름이 곧 계약이다 — BE `AfternoteCreateRequest.PlaylistRequest.memorialAudioUrl` (#248).
        val json =
            Json.encodeToString(
                MemorialWritePayload(
                    memorialPhotoUrl = null,
                    songs = emptyList(),
                    memorialVideo = null,
                    memorialAudioUrl = "https://cdn/voice.m4a",
                ).toDto(),
            )

        assertTrue(json.contains("\"memorialAudioUrl\":\"https://cdn/voice.m4a\""))
    }

    @Test
    fun `음성 미첨부면 memorialAudioUrl 을 명시적 null 로 보낸다`() {
        // 기본값이 없어야 키가 실린다. BE 는 «키 없음 = 유지 / JSON null = 삭제» 로 가르므로
        // 키가 빠지면 폼을 비워도 서버 음성을 지울 수 없다 (#1118, 규칙 정본은 #1596).
        val json =
            Json.encodeToString(
                MemorialWritePayload(
                    memorialPhotoUrl = null,
                    songs = emptyList(),
                    memorialVideo = null,
                    memorialAudioUrl = null,
                ).toDto(),
            )

        assertTrue(json.contains("\"memorialAudioUrl\":null"))
    }
}
