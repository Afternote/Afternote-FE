package com.afternote.afternote_fe.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.afternote_fe.usecase.GetHomeSummaryUseCase
import com.afternote.core.domain.repository.UserRepository
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
        private val getHomeSummary: GetHomeSummaryUseCase,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<HomeTabUiState>(HomeTabUiState.Loading())
        val uiState: StateFlow<HomeTabUiState> = _uiState.asStateFlow()

        /** 진행 중인 API 요청. 상태 대신 Job으로 가드하여 초기 Loading 딜레마를 회피한다. */
        private var fetchJob: Job? = null

        init {
            loadHomeSummary()
        }

        /**
         * 사용자가 요청한 로드 — 최초 진입, 당겨서 새로고침, 에러 재시도.
         *
         * @param isRefresh true이면 기존 데이터를 유지한 채 새로고침 스피너만 표시한다.
         */
        fun loadHomeSummary(isRefresh: Boolean = false) {
            // 이미 데이터가 있고 새로고침도 아니면 재요청하지 않는다.
            if (_uiState.value is HomeTabUiState.Success && !isRefresh) return

            fetch(showsRefreshingSpinner = isRefresh, keepsStateOnFailure = false)
        }

        /**
         * 다른 화면에서 홈으로 복귀했을 때의 자동 갱신.
         *
         * 사용자가 요청한 동작이 아니므로 [loadHomeSummary] 와 두 가지가 다르다.
         * - 상단 스피너를 띄우지 않는다. 홈은 왕복이 잦아 매번 스피너가 뜨면 체감이 크다.
         * - 실패해도 보고 있던 화면을 유지한다. 일시적 실패로 잘 보고 있던 홈이
         *   에러 화면으로 대체되면 사용자 입장에서는 인과가 설명되지 않는다.
         */
        fun refreshOnReturn() {
            fetch(showsRefreshingSpinner = false, keepsStateOnFailure = true)
        }

        private fun fetch(
            showsRefreshingSpinner: Boolean,
            keepsStateOnFailure: Boolean,
        ) {
            // Job이 아직 살아 있으면 중복 요청을 막는다.
            if (fetchJob?.isActive == true) return

            fetchJob =
                viewModelScope.launch {
                    val currentState = _uiState.value
                    if (currentState is HomeTabUiState.Success) {
                        // 기존 화면을 유지한다. 스피너는 사용자가 요청한 갱신에서만 표시한다.
                        if (showsRefreshingSpinner) {
                            _uiState.value = currentState.copy(isRefreshing = true)
                        }
                    } else {
                        // 초기 진입 또는 에러 재시도: 캐시된 이름이 있으면 placeholder로 즉시 노출한다.
                        _uiState.value =
                            HomeTabUiState.Loading()
                    }

                    getHomeSummary()
                        .onSuccess { summary ->
                            _uiState.value = summary.toHomeTabSuccess()
                        }.onFailure { error ->
                            _uiState.value =
                                if (keepsStateOnFailure && currentState is HomeTabUiState.Success) {
                                    currentState.copy(isRefreshing = false)
                                } else {
                                    HomeTabUiState.Error(error)
                                }
                        }
                }
        }
    }

private fun HomeSummary.toHomeTabSuccess(): HomeTabUiState.Success =
    HomeTabUiState.Success(
        userName = userName,
        isRecipientDesignated = isRecipientDesignated,
        todayQuestionContent = todayQuestionContent,
        categoryCounts =
            MindRecordCategory.entries.associateWith { category ->
                when (category) {
                    MindRecordCategory.DIARY -> diaryCategoryCount
                    MindRecordCategory.DAILY_QUESTION -> 0
                    MindRecordCategory.WEEKLY_REPORT -> 0
                }
            },
    )
