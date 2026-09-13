package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 곡 wire 변환 계약. 판정은 요청 바디에 실제로 실리는 단위인
 * [MemorialWritePayload.toDto] 의 `songs` 로 한다 — 곡 하나짜리 매퍼는 그 안의 구현 세부다.
 */
class PlaylistInputToDtoTest {
    @Test
    fun `곡 입력은 서버 계약의 제목 아티스트 커버만 wire로 변환한다`() {
        val dtoSong =
            MemorialWritePayload(
                memorialPhotoUrl = null,
                songs =
                    listOf(
                        MemorialSongPayload(
                            title = "노래",
                            artist = "가수",
                            coverUrl = "cover",
                        ),
                    ),
                memorialVideo = null,
            ).toDto()
                .songs
                .single()

        assertEquals("노래", dtoSong.title)
        assertEquals("가수", dtoSong.artist)
        assertEquals("cover", dtoSong.coverUrl)
    }
}
