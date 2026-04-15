package com.afternote.feature.afternote.presentation.author.editor.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song

/**
 * 추모 플레이리스트 상태 홀더
 */
@Stable
class MemorialPlaylistStateHolder {
    val songs: SnapshotStateList<Song> =
        mutableStateListOf()

    var onSongCountChanged: (() -> Unit)? = null

    fun initializeSongs(initialSongs: List<Song>) {
        if (songs.isEmpty()) {
            songs.addAll(initialSongs)
        }
    }

    fun addSong(song: Song) {
        songs.add(song)
        onSongCountChanged?.invoke()
    }

    @Suppress("UNUSED")
    fun removeSong(songId: String) {
        songs.removeAll { it.id == songId }
        onSongCountChanged?.invoke()
    }

    /**
     * 선택된 곡 ID 집합에 해당하는 곡들을 일괄 삭제합니다.
     */
    fun removeSongs(ids: Set<String>) {
        songs.removeAll { it.id in ids }
        onSongCountChanged?.invoke()
    }

    fun clearAllSongs() {
        songs.clear()
        onSongCountChanged?.invoke()
    }
}
