package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.presentation.editor.memorial.Song
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SongSerializationTest {
    @Test
    fun `곡의 UI 선택 키는 직렬화 후에도 유지된다`() {
        val songs =
            listOf(
                Song(
                    selectionKey = "detail:0",
                    title = "기존 노래",
                    artist = "가수",
                    albumCoverUrl = null,
                ),
                Song(
                    selectionKey = "search:가수|검색한 노래|0",
                    title = "검색한 노래",
                    artist = "가수",
                    albumCoverUrl = null,
                ),
            )

        val restored = Json.decodeFromString<List<Song>>(Json.encodeToString(songs))

        assertEquals(songs, restored)
        assertEquals(listOf("detail:0", "search:가수|검색한 노래|0"), restored.map(Song::selectionKey))
    }
}
