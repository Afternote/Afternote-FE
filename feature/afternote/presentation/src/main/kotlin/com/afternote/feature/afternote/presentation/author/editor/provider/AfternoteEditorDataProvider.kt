package com.afternote.feature.afternote.presentation.author.editor.provider
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.playlist.Song
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * Provides data for afternote edit flows (songs, receivers, album covers, afternote listItems).
 * Implementation (real vs dummy-backed) is decided at DI; consumers use this interface only.
 */
interface AfternoteEditorDataProvider {
    fun getSongs(): List<Song>

    fun getAfternoteEditorReceivers(): List<AfternoteEditorReceiver>

    fun getDefaultAfternoteItems(): List<Pair<String, String>>

    fun getAlbumCovers(): List<AlbumCover>

    /**
     * Search results for Add Song screen. Real impl uses API; fake returns dummies until API is ready.
     */
    fun getAddSongSearchResults(): List<PlaylistSongDisplay>
}
