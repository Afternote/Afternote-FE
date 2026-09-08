package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.receiver.navigation.ReceivedAfternoteRoute
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 수신 애프터노트 상세 ViewModel.
 *
 * - 상세 조회: [ReceiverRepository.getReceivedAfternoteDetail] (Retrofit baseUrl 기준
 *   `receiver-auth/after-notes/{afternoteId}` 경로 — 실제 경로는 data 모듈의 `ReceiverAfternoteApiService`).
 * - 상세 ID: [SavedStateHandle] 의 `afternoteId` (수신자 라우트 인자명).
 *
 * 발신자 [com.afternote.feature.afternote.presentation.detail.AfternoteDetailViewModel] 과
 * 동일한 단일 [ReceivedAfternoteDetailUiState] + StateFlow 패턴을 따른다. 다만 받은 입장이라
 * 수정·삭제·작성자 표시명·수신자 목록은 보유하지 않는다.
 */
@HiltViewModel(assistedFactory = ReceivedAfternoteDetailViewModel.Factory::class)
class ReceivedAfternoteDetailViewModel
    @AssistedInject
    constructor(
        @Assisted private val route: ReceivedAfternoteRoute.DetailRoute,
        private val receiverRepository: ReceiverRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val afternoteIdFromNav: Long =
            route.afternoteId

        private val _uiState =
            MutableStateFlow<ReceivedAfternoteDetailUiState>(ReceivedAfternoteDetailUiState.Loading)
        val uiState: StateFlow<ReceivedAfternoteDetailUiState> = _uiState.asStateFlow()

        /** 진행 중인 상세 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — Job 가드만으로는 init 로드가 (특히 실패로) 빨리 끝난 뒤
         * 도착한 첫 resume 이 순차 재조회를 걸어, 에러 화면과 «다시 시도하기» 가 통째로
         * 건너뛰어진다 (#701 CI 회귀). VM 필드인 이유는 [AfternoteDetailViewModel] 과 동일 —
         * 프로세스 사망 후 복원에서도 init 로드와 수명이 일치한다.
         */
        private var isFirstResume = true

        init {
            loadDetail(afternoteIdFromNav)
        }

        fun retry() {
            loadDetail(afternoteIdFromNav)
        }

        /**
         * 다른 화면에서 상세로 복귀했을 때의 자동 갱신 (#701).
         *
         * [retry] 와 두 가지가 다르다 — 로딩을 방출하지 않고, 실패해도 보고 있던 상세를 유지한다.
         * 첫 ON_RESUME(진입 자체)은 [isFirstResume] 로 스킵하고, 그 이후의 resume 이 실행 중인
         * 로드와 겹치면 진행 중인 Job 으로 건너뛴다.
         */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            if (loadJob?.isActive == true) return
            loadDetail(afternoteIdFromNav, showsLoading = false, keepsStateOnFailure = true)
        }

        private fun loadDetail(
            afternoteId: Long,
            showsLoading: Boolean = true,
            keepsStateOnFailure: Boolean = false,
        ) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    if (showsLoading) {
                        _uiState.value = ReceivedAfternoteDetailUiState.Loading
                    }
                    val result = receiverRepository.getReceivedAfternoteDetail(afternoteId = afternoteId)
                    // 새 로드가 이 Job 을 취소했다면 화면은 그쪽이 결정한다. repository 는
                    // `runCatchingCancellable` 로 취소를 다시 던져 대개 여기까지 오지 않지만, 조회가
                    // 값으로 끝난 뒤에 취소가 들어오면 취소된 로드가 그대로 새 화면을 덮는다 —
                    // «다시 시도하기» 가 진행 중인 자동 갱신을 자르는 순간이 그 창이다.
                    ensureActive()
                    result
                        .onSuccess { detail ->
                            _uiState.value =
                                ReceivedAfternoteDetailUiState.Success(
                                    detailId = afternoteId,
                                    contentUiModel = detail.toReceivedDetailContentUiModel(),
                                )
                        }.onFailure { e ->
                            // 화면을 유지하는 자동 갱신 실패도 기록한다 — 콘솔이 유일한 관측 지점이다.
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVED_DETAIL_LOAD, e)
                            _uiState.update { current ->
                                if (keepsStateOnFailure && current is ReceivedAfternoteDetailUiState.Success) {
                                    current
                                } else {
                                    ReceivedAfternoteDetailUiState.Error(
                                        messageRes = R.string.afternote_detail_load_error,
                                    )
                                }
                            }
                        }
                }
        }

        @AssistedFactory
        interface Factory {
            fun create(route: ReceivedAfternoteRoute.DetailRoute): ReceivedAfternoteDetailViewModel
        }
    }
