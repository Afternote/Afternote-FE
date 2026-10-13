package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.afternote.domain.repository.author.MusicSearchRepository
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_DEBOUNCE_MS = 300L

@HiltViewModel
internal class AddSongViewModel
    @Inject
    constructor(
        private val musicSearchRepository: MusicSearchRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<AddSongIntent, AddSongUiState, AddSongReducerEvent>(AddSongUiState()) {
        private var searchJob: Job? = null

        override fun onIntent(intent: AddSongIntent) {
            when (intent) {
                is AddSongIntent.Search -> search(intent.query)
                AddSongIntent.ConsumeError -> dispatch(AddSongReducerEvent.ErrorConsumed)
            }
        }

        private fun search(query: String) {
            dispatch(AddSongReducerEvent.QueryChanged(query))
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS.milliseconds)
                    val trimmed = query.trim()
                    if (trimmed.isEmpty()) {
                        dispatch(AddSongReducerEvent.SongsLoaded(emptyList()))
                        return@launch
                    }
                    dispatch(AddSongReducerEvent.SearchStarted)
                    val result = musicSearchRepository.search(trimmed)
                    ensureActive()
                    result
                        .onSuccess { list ->
                            dispatch(AddSongReducerEvent.SongsLoaded(list.map { it.toDisplay() }))
                        }.onFailure { error ->
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.MUSIC_SEARCH, error)
                            dispatch(AddSongReducerEvent.SearchFailed)
                        }
                }
        }

        override fun reduce(
            state: AddSongUiState,
            event: AddSongReducerEvent,
        ): AddSongUiState =
            when (event) {
                is AddSongReducerEvent.QueryChanged -> {
                    state.copy(searchQuery = event.query, errorRes = null)
                }

                AddSongReducerEvent.SearchStarted -> {
                    state.copy(isLoading = true)
                }

                is AddSongReducerEvent.SongsLoaded -> {
                    state.copy(songs = event.songs, isLoading = false, errorRes = null)
                }

                AddSongReducerEvent.SearchFailed -> {
                    state.copy(
                        songs = emptyList(),
                        isLoading = false,
                        errorRes = R.string.afternote_editor_search_failed_generic,
                    )
                }

                AddSongReducerEvent.ErrorConsumed -> {
                    state.copy(errorRes = null)
                }
            }
    }
