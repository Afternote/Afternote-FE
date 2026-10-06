package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.receiver.navigation.ReceivedAfternoteRoute
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 수신자 추억 플레이리스트 화면 ViewModel.
 *
 * 라우트의 afternoteId로 상세를 조회하여 playlist.songs를 [PlaylistSongDisplay]로 표시합니다.
 * `X-Auth-Code` 헤더는 ReceiverAuthInterceptor가 자동 부착합니다.
 */
@HiltViewModel(assistedFactory = ReceiverMemorialPlaylistViewModel.Factory::class)
internal class ReceiverMemorialPlaylistViewModel
    @AssistedInject
    constructor(
        @Assisted private val route: ReceivedAfternoteRoute.MemorialPlaylistRoute,
        private val receiverRepository: ReceiverRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<ReceiverMemorialPlaylistIntent, ReceiverMemorialPlaylistUiState, ReceiverMemorialPlaylistReducerEvent>(
            ReceiverMemorialPlaylistUiState.Loading,
        ) {
        private val afternoteIdFromNav: Long =
            route.afternoteId

        override fun onIntent(intent: ReceiverMemorialPlaylistIntent) {
            when (intent) {
                ReceiverMemorialPlaylistIntent.Retry -> retry()
                ReceiverMemorialPlaylistIntent.RefreshOnReturn -> refreshOnReturn()
            }
        }

        override fun reduce(
            state: ReceiverMemorialPlaylistUiState,
            event: ReceiverMemorialPlaylistReducerEvent,
        ): ReceiverMemorialPlaylistUiState =
            when (event) {
                ReceiverMemorialPlaylistReducerEvent.Loading -> {
                    ReceiverMemorialPlaylistUiState.Loading
                }

                is ReceiverMemorialPlaylistReducerEvent.ContentLoaded -> {
                    event.content
                }

                is ReceiverMemorialPlaylistReducerEvent.LoadFailed -> {
                    if (event.keepsContent &&
                        state is ReceiverMemorialPlaylistUiState.Success
                    ) {
                        state
                    } else {
                        ReceiverMemorialPlaylistUiState.Error(R.string.afternote_receiver_memorial_playlist_load_error)
                    }
                }
            }

        /** 진행 중인 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — Job 가드만으로는 init 로드가 (특히 실패로) 빨리 끝난 뒤
         * 도착한 첫 resume 이 순차 재조회를 걸어, 에러 화면과 재시도가 건너뛰어진다.
         * VM 필드인 이유는 [com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel]
         * 과 동일 — 프로세스 사망 후 복원에서도 init 로드와 수명이 일치한다.
         */
        private var isFirstResume = true

        init {
            loadPlaylist(afternoteIdFromNav)
        }

        private fun retry() {
            loadPlaylist(afternoteIdFromNav)
        }

        /**
         * 다른 화면에서 복귀했을 때의 자동 갱신 (#701).
         *
         * [retry] 와 두 가지가 다르다 — 로딩을 방출하지 않고, 실패해도 보고 있던 목록을 유지한다.
         * 첫 ON_RESUME(진입 자체)은 [isFirstResume] 로 스킵하고, 그 이후의 resume 이 실행 중인
         * 로드와 겹치면 진행 중인 Job 으로 건너뛴다.
         */
        private fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
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
                dispatch(ReceiverMemorialPlaylistReducerEvent.Loading)
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
                            dispatch(
                                ReceiverMemorialPlaylistReducerEvent.ContentLoaded(
                                    ReceiverMemorialPlaylistUiState.Success(
                                        senderName = detail.senderName,
                                        songs = songs,
                                        memorialVideoUrl = playlist?.memorialVideoUrl,
                                        memorialThumbnailUrl = playlist?.memorialThumbnailUrl,
                                    ),
                                ),
                            )
                        }.onFailure { e ->
                            // 화면을 유지하는 자동 갱신 실패도 기록한다 — 콘솔이 유일한 관측 지점이다.
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVED_PLAYLIST_LOAD, e)
                            dispatch(ReceiverMemorialPlaylistReducerEvent.LoadFailed(keepsStateOnFailure))
                        }
                }
        }

        @AssistedFactory
        interface Factory {
            fun create(route: ReceivedAfternoteRoute.MemorialPlaylistRoute): ReceiverMemorialPlaylistViewModel
        }
    }
