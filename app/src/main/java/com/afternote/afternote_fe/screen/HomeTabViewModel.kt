package com.afternote.afternote_fe.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.HomeRepository
import com.afternote.core.model.HomeSummary
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

        /**
         * @param isRefresh true이면 이미 로딩 중이어도 다시 요청한다(당겨서 새로고침 등).
         */
        fun loadHomeSummary(isRefresh: Boolean = false) {
            if (_uiState.value.isLoading && !isRefresh) return

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, isError = false) }

                homeRepository
                    .getHomeSummary()
                    .onSuccess { summary ->
                        _uiState.update { it.toSuccessState(summary) }
                    }.onFailure {
                        _uiState.update { it.toFailureState() }
                    }
            }
        }
    }

/** [HomeSummary] → 홈 탭 UI 성공 상태. */
private fun HomeTabUiState.toSuccessState(summary: HomeSummary): HomeTabUiState =
    copy(
        userName = summary.userName,
        isRecipientDesignated = summary.isRecipientDesignated,
        categoryCounts =
            MindRecordCategory.entries.associateWith { category ->
                when (category) {
                    MindRecordCategory.DIARY -> summary.diaryCategoryCount
                    MindRecordCategory.DEEP_THOUGHT -> summary.deepThoughtCategoryCount
                    MindRecordCategory.DAILY_QUESTION -> 0
                }
            },
        isLoading = false,
        isError = false,
    )

/**
 * 요청 실패 시: 로딩만 끄고 에러 플래그만 올린다.
 */
private fun HomeTabUiState.toFailureState(): HomeTabUiState =
    copy(
        isLoading = false,
        isError = true,
    )
