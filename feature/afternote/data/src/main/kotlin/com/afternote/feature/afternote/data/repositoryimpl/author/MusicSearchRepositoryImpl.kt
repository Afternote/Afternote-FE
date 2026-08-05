package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.feature.afternote.data.dto.MusicTrackDto
import com.afternote.feature.afternote.data.service.MusicApiService
import com.afternote.feature.afternote.domain.model.author.playlist.SearchedSong
import com.afternote.feature.afternote.domain.repository.author.MusicSearchRepository
import kotlinx.coroutines.CancellationException
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
            return try {
                val tracks = api.search(keyword = trimmed).tracks
                Result.success(tracks.mapIndexed { index, dto -> dto.toPlaylistSongDisplay(index) })
            } catch (e: CancellationException) {
                // runCatching 은 in-flight 취소까지 Result.failure 로 삼켜, 타이핑으로 이전 검색이
                // 취소될 때마다 유령 "검색 실패"가 화면에 남았다. 취소는 실패가 아니므로 되던진다.
                throw e
            } catch (e: Exception) {
                Result.failure(e)
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
