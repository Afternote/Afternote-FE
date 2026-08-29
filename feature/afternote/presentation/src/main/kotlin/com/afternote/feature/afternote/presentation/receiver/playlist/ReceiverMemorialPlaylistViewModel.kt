package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceivedAfternoteRoute
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 추억 플레이리스트 화면 ViewModel.
 *
 * 라우트의 afternoteId로 상세를 조회하여 playlist.songs를 [PlaylistSongDisplay]로 표시합니다.
 * `X-Auth-Code` 헤더는 ReceiverAuthInterceptor가 자동 부착합니다.
 */
@HiltViewModel
class ReceiverMemorialPlaylistViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val receiverRepository: ReceiverRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val afternoteIdFromNav: Long =
            savedStateHandle.toRoute<ReceivedAfternoteRoute.MemorialPlaylistRoute>().afternoteId

        private val _uiState =
            MutableStateFlow<ReceiverMemorialPlaylistUiState>(ReceiverMemorialPlaylistUiState.Loading)
        val uiState: StateFlow<ReceiverMemorialPlaylistUiState> = _uiState.asStateFlow()

        init {
            loadPlaylist(afternoteIdFromNav)
        }

        fun retry() {
            loadPlaylist(afternoteIdFromNav)
        }

        private fun loadPlaylist(afternoteId: Long) {
            _uiState.value = ReceiverMemorialPlaylistUiState.Loading
            viewModelScope.launch {
                receiverRepository
                    .getReceivedAfternoteDetail(afternoteId = afternoteId)
                    .onSuccess { detail ->
                        val playlist = detail.playlist
                        val songs =
                            playlist
                                ?.songs
                                ?.mapIndexed { index, song ->
                                    PlaylistSongDisplay(
                                        selectionKey = "received:$index",
                                        title = song.title,
                                        artist = song.artist,
                                        albumImageUrl = song.coverUrl,
                                    )
                                }.orEmpty()
                        _uiState.value =
                            ReceiverMemorialPlaylistUiState.Success(
                                senderName = detail.senderName.orEmpty(),
                                songs = songs,
                                memorialVideoUrl = playlist?.memorialVideoUrl,
                                memorialThumbnailUrl = playlist?.memorialThumbnailUrl,
                            )
                    }.onFailure { e ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVED_PLAYLIST_LOAD, e)
                        _uiState.value =
                            ReceiverMemorialPlaylistUiState.Error(
                                messageRes = R.string.receiver_memorial_playlist_load_error,
                            )
                    }
            }
        }
    }
