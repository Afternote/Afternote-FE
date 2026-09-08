package com.afternote.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.feature.home.presentation.reporting.HomeFailureStage
import com.afternote.feature.home.presentation.reporting.recordHomeFailure
import com.afternote.feature.home.presentation.usecase.GetHomeSummaryUseCase
import com.afternote.feature.home.presentation.usecase.HomeSummary
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory
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
        private val userProfileCacheRepository: UserProfileCacheRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<HomeTabUiState>(HomeTabUiState.Loading())
        val uiState: StateFlow<HomeTabUiState> = _uiState.asStateFlow()

        /** 진행 중인 API 요청. 상태 대신 Job으로 가드하여 초기 Loading 딜레마를 회피한다. */
        private var fetchJob: Job? = null

        /** [fetchJob] 이 사용자가 직접 요청한 로드인지. 자동 갱신과의 우선순위를 가른다. */
        private var isUserRequestedFetch = false

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

            fetch(showsRefreshingSpinner = isRefresh, keepsStateOnFailure = false, isUserRequested = true)
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
            fetch(showsRefreshingSpinner = false, keepsStateOnFailure = true, isUserRequested = false)
        }

        /**
         * @param isUserRequested 당겨서 새로고침·재시도처럼 사용자가 직접 일으킨 로드인지.
         *   자동 갱신과 겹쳤을 때 어느 쪽이 살아남는지를 가른다.
         */
        private fun fetch(
            showsRefreshingSpinner: Boolean,
            keepsStateOnFailure: Boolean,
            isUserRequested: Boolean,
        ) {
            if (fetchJob?.isActive == true) {
                // 사용자 요청끼리 겹치면 뒤엣것을 버린다 — 앞선 요청의 스피너가 이미 떠 있어 무음이 아니다.
                // 반대로 응답 없이 매달린 자동 갱신에는 자리를 내주게 한다. 그러지 않으면 서버가
                // 무응답인 동안 "다시 시도"·당겨서 새로고침이 통째로 삼켜진다.
                if (isUserRequestedFetch || !isUserRequested) return
                fetchJob?.cancel()
            }
            isUserRequestedFetch = isUserRequested

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
                            HomeTabUiState.Loading(
                                cachedUserName = userProfileCacheRepository.getCachedUserName(),
                                showsRefreshIndicator = showsRefreshingSpinner,
                            )
                    }

                    getHomeSummary()
                        .onSuccess { summary ->
                            _uiState.value = summary.toHomeTabSuccess()
                            cacheUserName(summary.userName)
                        }.onFailure { error ->
                            // 화면을 유지하는 자동 갱신 실패도 기록한다. 사용자에게 안 보이는 만큼
                            // 콘솔이 유일한 관측 지점이다.
                            errorReporter.recordHomeFailure(HomeFailureStage.AUTHOR_SUMMARY_LOAD, error)
                            _uiState.value =
                                if (keepsStateOnFailure && currentState is HomeTabUiState.Success) {
                                    currentState.copy(isRefreshing = false)
                                } else {
                                    HomeTabUiState.Error(error)
                                }
                        }
                }
        }

        /**
         * 다음 콜드스타트에서 placeholder 로 쓸 이름을 디스크에 남긴다.
         *
         * 저장 실패는 화면에 반영하지 않는다 — 이름은 이미 응답으로 표시돼 있고, 손실은
         * 다음 진입의 placeholder 가 한 번 더 비는 것뿐이다.
         */
        private suspend fun cacheUserName(name: String) {
            runCatchingCancellable { userProfileCacheRepository.saveUserName(name) }
        }
    }

private fun HomeSummary.toHomeTabSuccess(): HomeTabUiState.Success =
    HomeTabUiState.Success(
        userName = userName,
        isRecipientDesignated = isRecipientDesignated,
        todayQuestionContent = todayQuestionContent,
        weeklyRecordCount = weeklyRecordCount,
    )
