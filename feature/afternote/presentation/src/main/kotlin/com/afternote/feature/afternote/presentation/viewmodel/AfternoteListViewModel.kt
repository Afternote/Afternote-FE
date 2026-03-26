package com.afternote.feature.afternote.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.domain.model.input.GetListPageInput
import com.afternote.feature.afternote.domain.usecase.GetListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AfternoteListUiState {
    data object Loading : AfternoteListUiState

    data class Success(
        val items: List<Item>,
        val hasNext: Boolean,
        val selectedType: AfternoteServiceType?,
    ) : AfternoteListUiState

    data class Error(val message: String) : AfternoteListUiState
}

@HiltViewModel
class AfternoteListViewModel
    @Inject
    constructor(
        private val getListUseCase: GetListUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AfternoteListUiState>(AfternoteListUiState.Loading)
        val uiState: StateFlow<AfternoteListUiState> = _uiState.asStateFlow()

        private var currentPage = 0
        private var selectedType: AfternoteServiceType? = null

        init {
            loadList()
        }

        fun loadList(type: AfternoteServiceType? = selectedType) {
            selectedType = type
            currentPage = 0
            _uiState.value = AfternoteListUiState.Loading
            fetchPage(page = 0, type = type)
        }

        private fun fetchPage(
            page: Int,
            type: AfternoteServiceType?,
        ) {
            viewModelScope.launch {
                val category =
                    when (type) {
                        AfternoteServiceType.SOCIAL_NETWORK -> "SOCIAL"
                        AfternoteServiceType.GALLERY_AND_FILES -> "GALLERY"
                        AfternoteServiceType.MEMORIAL -> "PLAYLIST"
                        null -> null
                    }
                val input = GetListPageInput(category = category, page = page, size = PAGE_SIZE)
                getListUseCase(input).fold(
                    onSuccess = { listPage ->
                        currentPage = page
                        _uiState.value =
                            AfternoteListUiState.Success(
                                items = listPage.items,
                                hasNext = listPage.hasNext,
                                selectedType = type,
                            )
                    },
                    onFailure = { error ->
                        _uiState.value = AfternoteListUiState.Error(error.message ?: "Unknown error")
                    },
                )
            }
        }

        fun loadNextPage() {
            val state = _uiState.value as? AfternoteListUiState.Success ?: return
            if (!state.hasNext) return
            viewModelScope.launch {
                val nextPage = currentPage + 1
                val category =
                    when (selectedType) {
                        AfternoteServiceType.SOCIAL_NETWORK -> "SOCIAL"
                        AfternoteServiceType.GALLERY_AND_FILES -> "GALLERY"
                        AfternoteServiceType.MEMORIAL -> "PLAYLIST"
                        null -> null
                    }
                val input = GetListPageInput(category = category, page = nextPage, size = PAGE_SIZE)
                getListUseCase(input).fold(
                    onSuccess = { listPage ->
                        currentPage = nextPage
                        _uiState.value =
                            AfternoteListUiState.Success(
                                items = state.items + listPage.items,
                                hasNext = listPage.hasNext,
                                selectedType = selectedType,
                            )
                    },
                    onFailure = { error ->
                        _uiState.value = AfternoteListUiState.Error(error.message ?: "Unknown error")
                    },
                )
            }
        }

        companion object {
            private const val PAGE_SIZE = 10
        }
    }
