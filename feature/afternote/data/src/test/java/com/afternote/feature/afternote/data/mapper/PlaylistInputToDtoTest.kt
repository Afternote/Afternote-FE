package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistInputToDtoTest {
    @Test
    fun `곡 입력은 서버 계약의 제목 아티스트 커버만 wire로 변환한다`() {
        val dtoSongs =
            listOf(
                MemorialSongPayload(
                    title = "노래",
                    artist = "가수",
                    coverUrl = "cover",
                ),
            ).toDto()

        assertEquals("노래", dtoSongs.single().title)
        assertEquals("가수", dtoSongs.single().artist)
        assertEquals("cover", dtoSongs.single().coverUrl)
    }
}
