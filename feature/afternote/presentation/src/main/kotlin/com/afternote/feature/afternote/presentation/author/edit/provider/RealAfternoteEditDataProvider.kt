package com.afternote.feature.afternote.presentation.author.edit.provider
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiver
import com.afternote.feature.afternote.presentation.author.edit.playlist.Song
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import javax.inject.Inject

/**
 * Real implementation. Returns empty/placeholder until API is available.
 */
class RealAfternoteEditDataProvider
    @Inject
    constructor() : AfternoteEditDataProvider {
        override fun getSongs(): List<Song> = emptyList()

        override fun getAfternoteEditReceivers(): List<AfternoteEditReceiver> = emptyList()

        override fun getDefaultAfternoteItems(): List<Pair<String, String>> = emptyList()

        override fun getAlbumCovers(): List<AlbumCover> = emptyList()

        override fun getAddSongSearchResults(): List<PlaylistSongDisplay> = emptyList()
    }
