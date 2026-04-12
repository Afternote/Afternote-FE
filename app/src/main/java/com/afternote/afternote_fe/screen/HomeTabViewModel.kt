package com.afternote.afternote_fe.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.HomeRepository
import com.afternote.core.model.MindRecordCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeTabViewModel
    @Inject
    constructor(
        private val homeRepository: HomeRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeTabUiState())
        val uiState: StateFlow<HomeTabUiState> = _uiState.asStateFlow()

        init {
            loadHomeSummary()
        }

        fun loadHomeSummary() {
            if (_uiState.value.isLoading) return

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, isError = false) }

                homeRepository
                    .getHomeSummary()
                    .onSuccess { data ->
                        _uiState.update {
                            it.copy(
                                userName = data.userName,
                                isRecipientDesignated = data.isRecipientDesignated,
                                categoryCounts =
                                    mapOf(
                                        MindRecordCategory.DIARY to data.diaryCategoryCount,
                                        MindRecordCategory.DEEP_THOUGHT to data.deepThoughtCategoryCount,
                                    ),
                                isLoading = false,
                            )
                        }
                    }.onFailure {
                        _uiState.update {
                            it.copy(isLoading = false, isError = true)
                        }
                    }
            }
        }
    }
