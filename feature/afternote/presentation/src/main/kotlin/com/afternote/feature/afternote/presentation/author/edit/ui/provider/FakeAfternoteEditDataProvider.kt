package com.afternote.feature.afternote.presentation.author.edit.ui.provider
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiver
import com.afternote.feature.afternote.presentation.author.edit.model.Song
import com.afternote.feature.afternote.presentation.shared.component.list.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.dummy.AlbumDummies
import com.afternote.feature.afternote.presentation.shared.model.dummy.afternote.AfternoteEditDummies
import com.afternote.feature.afternote.presentation.shared.model.dummy.afternote.AfternoteListDummies
import com.afternote.feature.afternote.presentation.shared.model.uimodel.PlaylistSongDisplay
import javax.inject.Inject

/**
 * Fake implementation. Only place that imports and uses *Dummies.
 * Initial receivers are empty; Gallery uses Settings receiver list (GET /users/receivers).
 */
class FakeAfternoteEditDataProvider
    @Inject
    constructor() : AfternoteEditDataProvider {
        override fun getSongs(): List<Song> = AfternoteEditDummies.defaultSongs()

        override fun getAfternoteEditReceivers(): List<AfternoteEditReceiver> = emptyList()

        override fun getDefaultAfternoteItems(): List<Pair<String, String>> = AfternoteListDummies.defaultAfternoteList()

        override fun getAlbumCovers(): List<AlbumCover> = AlbumDummies.list

        override fun getAddSongSearchResults(): List<PlaylistSongDisplay> = AfternoteEditDummies.defaultAddSongSearchResults()
    }
