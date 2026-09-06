package com.afternote.feature.afternote.presentation.editor.memorial

import com.afternote.feature.afternote.domain.model.author.playlist.SearchedSong
import org.junit.Assert.assertEquals
import org.junit.Test

class SongMapperTest {
    @Test
    fun `검색 곡의 앨범 이미지 URL을 표시 모델에 보존한다`() {
        val albumImageUrl = "https://cdn.test/album-cover.jpg"

        val display =
            SearchedSong(
                selectionKey = "search:artist|title|0",
                title = "title",
                artist = "artist",
                albumImageUrl = albumImageUrl,
            ).toDisplay()

        assertEquals(albumImageUrl, display.albumImageUrl)
    }
}
