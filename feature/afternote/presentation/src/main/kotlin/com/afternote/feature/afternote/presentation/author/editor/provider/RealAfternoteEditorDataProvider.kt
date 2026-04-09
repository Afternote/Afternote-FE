package com.afternote.feature.afternote.presentation.author.editor.provider

import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.playlist.Song
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import javax.inject.Inject

/**
 * Real implementation. Returns empty/placeholder until API is available.
 */
class RealAfternoteEditorDataProvider
    @Inject
    constructor() : AfternoteEditorDataProvider {
        override fun getSongs(): List<Song> = emptyList()

        override fun getAfternoteEditorReceivers(): List<AfternoteEditorReceiver> = emptyList()

        override fun getDefaultAfternoteListItems(): List<ListItem> = emptyList()

        override fun getAlbumCovers(): List<AlbumCover> = emptyList()

        override fun getAddSongSearchResults(): List<PlaylistSongDisplay> = emptyList()
    }
