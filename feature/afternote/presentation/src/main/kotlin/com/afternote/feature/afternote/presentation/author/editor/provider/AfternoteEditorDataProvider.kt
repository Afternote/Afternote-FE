package com.afternote.feature.afternote.presentation.author.editor.provider
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.playlist.Song
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * Provides data for afternote edit flows (songs, receivers, album covers, afternote visibleItems).
 * Implementation (real vs dummy-backed) is decided at DI; consumers use this interface only.
 */
interface AfternoteEditorDataProvider {
    fun getSongs(): List<Song>

    fun getAfternoteEditorReceivers(): List<AfternoteEditorReceiver>

    /**
     * Fallback list when the editor opens without items from home (e.g. new draft).
     * Fake impl returns dummy [ListItem]s; real impl returns empty until API-backed defaults exist.
     */
    fun getDefaultAfternoteListItems(): List<ListItem>

    fun getAlbumCovers(): List<AlbumCover>

    /**
     * Search results for Add Song screen. Real impl uses API; fake returns dummies until API is ready.
     */
    fun getAddSongSearchResults(): List<PlaylistSongDisplay>
}
