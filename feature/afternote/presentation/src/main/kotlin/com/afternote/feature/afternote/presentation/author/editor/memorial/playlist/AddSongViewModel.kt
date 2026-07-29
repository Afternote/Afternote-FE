package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.repository.author.MusicSearchRepository
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_DEBOUNCE_MS = 300L

@HiltViewModel
class AddSongViewModel
    @Inject
    constructor(
        private val musicSearchRepository: MusicSearchRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddSongUiState())
        val uiState: StateFlow<AddSongUiState> = _uiState.asStateFlow()

        private var searchJob: Job? = null

        fun onSearchQueryChange(query: String) {
            _uiState.update { it.copy(searchQuery = query, error = null) }
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS.milliseconds)
                    val trimmed = query.trim()
                    if (trimmed.isEmpty()) {
                        _uiState.update { it.copy(songs = emptyList(), isLoading = false) }
                        return@launch
                    }
                    _uiState.update { it.copy(isLoading = true) }
                    musicSearchRepository
                        .search(trimmed)
                        .onSuccess { list ->
                            _uiState.update {
                                it.copy(songs = list.map { item -> item.toDisplay() }, isLoading = false, error = null)
                            }
                        }.onFailure { e ->
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.MUSIC_SEARCH, e)
                            _uiState.update {
                                it.copy(
                                    songs = emptyList(),
                                    isLoading = false,
                                    error =
                                        e.message
                                            ?.let { msg -> AddSongError.SearchFailedWithMessage(msg) }
                                            ?: AddSongError.SearchFailedGeneric,
                                )
                            }
                        }
                }
        }

        /** UI 가 [AddSongUiState.error] 를 사용자에게 노출 직후 호출 → state nullify. */
        fun onErrorConsumed() {
            _uiState.update { it.copy(error = null) }
        }
    }
