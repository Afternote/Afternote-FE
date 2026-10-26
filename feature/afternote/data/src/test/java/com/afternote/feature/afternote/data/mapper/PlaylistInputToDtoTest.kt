package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 곡 wire 변환 계약. 판정은 요청 바디 루트인 [CreateMemorialPayload.toRequest] 의 `memorial.songs` 로
 * 한다 — 추모 노트·곡 하나짜리 매퍼는 그 안의 구현 세부라 파일 밖으로 내지 않는다.
 */
class PlaylistInputToDtoTest {
    @Test
    fun `곡 입력은 서버 계약의 제목 아티스트 커버만 wire로 변환한다`() {
        val dtoSong =
            CreateMemorialPayload(
                title = "추억 노트",
                memorial =
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
                    ),
            ).toRequest()
                .memorial
                .songs
                .single()

        assertEquals("노래", dtoSong.title)
        assertEquals("가수", dtoSong.artist)
        assertEquals("cover", dtoSong.coverUrl)
    }
}
