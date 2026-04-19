package com.afternote.afternote_fe.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.HomeRepository
import com.afternote.core.model.HomeSummary
import com.afternote.core.model.MindRecordCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeTabViewModel
    @Inject
    constructor(
        private val homeRepository: HomeRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<HomeTabUiState>(HomeTabUiState.Loading)
        val uiState: StateFlow<HomeTabUiState> = _uiState.asStateFlow()

        /** 진행 중인 API 요청. 상태 대신 Job으로 가드하여 초기 Loading 딜레마를 회피한다. */
        private var fetchJob: Job? = null

        init {
            loadHomeSummary()
        }

        /**
         * @param isRefresh true이면 기존 데이터를 유지한 채 새로고침 스피너만 표시한다.
         */
        fun loadHomeSummary(isRefresh: Boolean = false) {
            // Job이 아직 살아 있으면 중복 요청을 막는다.
            if (fetchJob?.isActive == true) return

            // 이미 데이터가 있고 새로고침도 아니면 재요청하지 않는다.
            if (_uiState.value is HomeTabUiState.Success && !isRefresh) return

            fetchJob =
                viewModelScope.launch {
                    val currentState = _uiState.value
                    if (isRefresh && currentState is HomeTabUiState.Success) {
                        // 새로고침: 기존 화면을 유지하고 상단 스피너만 표시한다.
                        _uiState.value = currentState.copy(isRefreshing = true)
                    } else {
                        // 초기 진입 또는 에러 재시도: 전체 로딩 화면을 표시한다.
                        _uiState.value = HomeTabUiState.Loading
                    }

                    homeRepository
                        .getHomeSummary()
                        .onSuccess { summary ->
                            _uiState.value = summary.toHomeTabSuccess()
                        }.onFailure { error ->
                            _uiState.value = HomeTabUiState.Error(error)
                        }
                }
        }
    }

private fun HomeSummary.toHomeTabSuccess(): HomeTabUiState.Success =
    HomeTabUiState.Success(
        userName = userName,
        isRecipientDesignated = isRecipientDesignated,
        categoryCounts =
            MindRecordCategory.entries.associateWith { category ->
                when (category) {
                    MindRecordCategory.DIARY -> diaryCategoryCount
                    MindRecordCategory.DEEP_THOUGHT -> deepThoughtCategoryCount
                    MindRecordCategory.DAILY_QUESTION -> 0
                    MindRecordCategory.WEEKLY_REPORT -> 0
                }
            },
    )
