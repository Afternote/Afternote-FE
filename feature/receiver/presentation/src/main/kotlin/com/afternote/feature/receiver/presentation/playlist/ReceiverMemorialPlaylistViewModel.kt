package com.afternote.feature.receiver.presentation.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
            savedStateHandle.toRoute<ReceiverRoute.MemorialPlaylistRoute>().afternoteId

        private val _uiState =
            MutableStateFlow<ReceiverMemorialPlaylistUiState>(ReceiverMemorialPlaylistUiState.Loading)
        val uiState: StateFlow<ReceiverMemorialPlaylistUiState> = _uiState.asStateFlow()

        /** 진행 중인 조회. 최초 진입 ON_RESUME 과 init 로드의 중복을 이 Job 으로 가른다. */
        private var loadJob: Job? = null

        init {
            loadPlaylist(afternoteIdFromNav)
        }

        fun retry() {
            loadPlaylist(afternoteIdFromNav)
        }

        /**
         * 다른 화면에서 복귀했을 때의 자동 갱신 (#701).
         *
         * [retry] 와 두 가지가 다르다 — 로딩을 방출하지 않고, 실패해도 보고 있던 목록을 유지한다.
         * 진입 직후의 ON_RESUME 은 init 로드와 겹친다 — 진행 중이면 건너뛴다. 컴포지션 쪽 플래그가
         * 아니라 VM 이 들고 있는 Job 으로 판단해야 프로세스 사망 후 복원에서도 중복이 나지 않는다.
         */
        fun refreshOnReturn() {
            if (loadJob?.isActive == true) return
            loadPlaylist(afternoteIdFromNav, showsLoading = false, keepsStateOnFailure = true)
        }

        private fun loadPlaylist(
            afternoteId: Long,
            showsLoading: Boolean = true,
            keepsStateOnFailure: Boolean = false,
        ) {
            loadJob?.cancel()
            if (showsLoading) {
                _uiState.value = ReceiverMemorialPlaylistUiState.Loading
            }
            loadJob =
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
                            // 화면을 유지하는 자동 갱신 실패도 기록한다 — 콘솔이 유일한 관측 지점이다.
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVED_PLAYLIST_LOAD, e)
                            _uiState.update { current ->
                                if (keepsStateOnFailure && current is ReceiverMemorialPlaylistUiState.Success) {
                                    current
                                } else {
                                    ReceiverMemorialPlaylistUiState.Error(
                                        messageRes = R.string.receiver_memorial_playlist_load_error,
                                    )
                                }
                            }
                        }
                }
        }
    }
