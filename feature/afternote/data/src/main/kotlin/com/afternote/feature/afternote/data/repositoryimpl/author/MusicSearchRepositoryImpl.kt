package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.feature.afternote.data.dto.MusicTrackDto
import com.afternote.feature.afternote.data.service.MusicApiService
import com.afternote.feature.afternote.domain.model.author.playlist.SearchedSong
import com.afternote.feature.afternote.domain.repository.author.MusicSearchRepository
import javax.inject.Inject

/**
 * Data layer: calls music search API and maps DTO to domain [SearchedSong].
 * API returns raw { "tracks": [...] } (no BaseResponse wrapper).
 */
class MusicSearchRepositoryImpl
    @Inject
    constructor(
        private val api: MusicApiService,
    ) : MusicSearchRepository {
        override suspend fun search(keyword: String): Result<List<SearchedSong>> {
            val trimmed = keyword.trim()
            if (trimmed.isEmpty()) return Result.success(emptyList())
            return runCatchingCancellable {
                api.search(keyword = trimmed).tracks.mapIndexed { index, dto -> dto.toPlaylistSongDisplay(index) }
            }
        }

        private fun MusicTrackDto.toPlaylistSongDisplay(index: Int): SearchedSong {
            val id = "$artist|$title|$index"
            return SearchedSong(
                id = id,
                title = title,
                artist = artist,
                albumImageUrl = albumImageUrl,
            )
        }
    }
