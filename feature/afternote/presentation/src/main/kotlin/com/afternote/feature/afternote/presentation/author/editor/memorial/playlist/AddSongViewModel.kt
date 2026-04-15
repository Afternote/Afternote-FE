package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.repository.MusicSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 300L

@HiltViewModel
class AddSongViewModel
    @Inject
    constructor(
        private val musicSearchRepository: MusicSearchRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddSongUiState())
        val uiState: StateFlow<AddSongUiState> = _uiState.asStateFlow()

        private var searchJob: Job? = null

        // region Event

        fun onEvent(event: AddSongEvent) {
            when (event) {
                is AddSongEvent.SearchQueryChange -> handleSearchQueryChange(event.query)
            }
        }

        // endregion

        // region Data Loading

        private fun handleSearchQueryChange(query: String) {
            _uiState.update { it.copy(searchQuery = query, errorMessage = null) }
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
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
                                it.copy(songs = list.map { item -> item.toDisplay() }, isLoading = false, errorMessage = null)
                            }
                        }.onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    songs = emptyList(),
                                    isLoading = false,
                                    errorMessage = e.message ?: "검색 실패",
                                )
                            }
                        }
                }
        }

        // endregion
    }
