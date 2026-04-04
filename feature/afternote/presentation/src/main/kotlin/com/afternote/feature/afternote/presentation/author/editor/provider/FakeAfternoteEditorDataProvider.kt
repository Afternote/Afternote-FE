package com.afternote.feature.afternote.presentation.author.editor.provider

import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.playlist.Song
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import com.afternote.feature.afternote.presentation.shared.model.dummy.AlbumDummies
import com.afternote.feature.afternote.presentation.shared.model.dummy.afternote.AfternoteEditorDummies
import com.afternote.feature.afternote.presentation.shared.model.dummy.afternote.AfternoteListDummies
import javax.inject.Inject

/**
 * Fake implementation. Only place that imports and uses *Dummies.
 * Initial receivers are empty; Gallery uses Settings receiver list (GET /users/receivers).
 */
class FakeAfternoteEditorDataProvider
    @Inject
    constructor() : AfternoteEditorDataProvider {
        override fun getSongs(): List<Song> = AfternoteEditorDummies.defaultSongs()

        override fun getAfternoteEditorReceivers(): List<AfternoteEditorReceiver> = emptyList()

        override fun getDefaultAfternoteItems(): List<Pair<String, String>> = AfternoteListDummies.defaultAfternoteList()

        override fun getAlbumCovers(): List<AlbumCover> = AlbumDummies.list

        override fun getAddSongSearchResults(): List<PlaylistSongDisplay> = AfternoteEditorDummies.defaultAddSongSearchResults()
    }
