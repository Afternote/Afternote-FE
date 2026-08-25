package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.error.DeliveryNotReadyException
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.reporting.MindRecordFailureStage
import com.afternote.feature.mindrecord.presentation.reporting.recordMindRecordFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 마음의 기록 화면 ViewModel.
 *
 * `receiver-auth` 의 데일리질문/일기 2개 엔드포인트를 병렬 조회해 탭별로 노출한다.
 * 정렬·기간 필터는 클라이언트에서 [ReceiverMindRecordFilter] 로 처리한다.
 */
@HiltViewModel
class ReceiverMindRecordViewModel
    @Inject
    constructor(
        private val repository: MindRecordReceiverRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val rawRecords = MutableStateFlow<ReceiverMindRecords?>(null)
        private val _uiState = MutableStateFlow<ReceiverMindRecordUiState>(ReceiverMindRecordUiState.Loading)
        val uiState: StateFlow<ReceiverMindRecordUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            load()
        }

        /**
         * 화면 재진입 갱신.
         *
         * 로딩을 방출하지 않는다 — ON_RESUME 은 화면이 살아 있는 채로 발화하므로, 스피너로
         * 갈아치우면 보고 있던 목록과 필터가 사라진다. 실패해도 기존 화면을 유지한다.
         */
        fun refreshOnReturn() {
            if (loadJob?.isActive == true) return
            load(showsLoading = false)
        }

        /** 오류 화면의 재시도 — 이쪽은 로딩을 보여야 한다. 보고 있던 것이 오류 문구뿐이라 잃을 게 없다. */
        fun refresh() = load()

        fun applyFilter(filter: ReceiverMindRecordFilter) {
            _uiState.update { current ->
                if (current is ReceiverMindRecordUiState.Success) {
                    current.copy(filter = filter).withDerived()
                } else {
                    current
                }
            }
        }

        fun resetFilter() = applyFilter(ReceiverMindRecordFilter())

        private fun load(showsLoading: Boolean = true) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    if (showsLoading) _uiState.update { ReceiverMindRecordUiState.Loading }
                    repository
                        .getAll()
                        .onSuccess { records ->
                            rawRecords.value = records
                            // 보고 있던 필터·정렬을 승계한다. 새 Success 를 기본값으로 만들면
                            // 재진입 갱신이 성공할 때마다 정렬·기간 필터가 조용히 초기화된다 —
                            // 종전에는 load() 가 init·재시도에서만 돌아 이 경로가 없었고,
                            // 이 PR 의 refreshOnReturn() 이 그 경로를 처음 만든다 (리뷰 지적).
                            val currentFilter =
                                (_uiState.value as? ReceiverMindRecordUiState.Success)?.filter
                                    ?: ReceiverMindRecordFilter()
                            _uiState.value =
                                ReceiverMindRecordUiState
                                    .Success(
                                        dailyQuestions = emptyList(),
                                        diaries = emptyList(),
                                        filter = currentFilter,
                                    ).withDerived()
                        }.onFailure { e ->
                            // 유가족이 «지금 못 여는» 상황이라 읽기 실패지만 승격 대상이다 —
                            // 재현할 계정도 조건도 우리 손에 없어 실기 QA 로는 잡히지 않는다 (#964).
                            // 재진입 갱신 실패도 기록한다. 화면은 유지되지만 서버 상태는 실패다.
                            //
                            // 다만 전달 조건 미충족은 **정상 상태**다 — 서버도 기기도 멀쩡하고
                            // 발신자가 조건을 정해야 풀린다. 전달을 기다리는 수신자는 앱을 열
                            // 때마다 이 경로를 타므로, 기록하면 보관 한도(최근 8건)를 그 잡음이
                            // 채워 정작 잡아야 할 열람 실패를 밀어낸다 (#964 판단 기준: 유지).
                            if (e !is DeliveryNotReadyException) {
                                errorReporter.recordMindRecordFailure(MindRecordFailureStage.RECEIVER_RECORD_LOAD, e)
                            }
                            // 재진입 갱신 실패는 보고 있던 화면을 유지한다.
                            if (showsLoading) {
                                // 서버 원문을 화면 문구로 쓰지 않는다 — 타입에서 막는다 (#614).
                                _uiState.value =
                                    ReceiverMindRecordUiState.Error(message = e.toDomainMessage())
                            }
                        }
                }
        }

        /**
         * 필터 변경 시 파생 list 2종을 재계산해 반환한다.
         * 수신자 view 는 임시저장 record 를 보지 않는다 — 서버가 제외하기를 기대하나 방어적으로 한 번 더 거른다.
         */
        private fun ReceiverMindRecordUiState.Success.withDerived(): ReceiverMindRecordUiState.Success {
            val records = rawRecords.value ?: return this

            fun List<MindRecordSummary>.derived(): List<MindRecordSummary> =
                filterNot { it.isDraft }
                    .filterByDate(filter.fromDate, filter.toDate)
                    .sortBy(filter.sortOrder)

            return copy(
                dailyQuestions = records.dailyQuestions.derived(),
                diaries = records.diaries.derived(),
            )
        }

        companion object {
            private fun List<MindRecordSummary>.filterByDate(
                from: String?,
                to: String?,
            ): List<MindRecordSummary> =
                filter { record ->
                    (from == null || record.recordDate >= from) && (to == null || record.recordDate <= to)
                }

            private fun List<MindRecordSummary>.sortBy(order: SortOrder): List<MindRecordSummary> =
                when (order) {
                    SortOrder.NEWEST -> sortedByDescending { it.recordDate }
                    SortOrder.OLDEST -> sortedBy { it.recordDate }
                }
        }
    }

/**
 * 실패를 화면 문구로 바꾼다 (#614).
 *
 * 서버 에러 코드는 보지 않는다 — 그 판정은 data 계층(`mapReceiverFailure`)이 하고 여기는
 * 도메인 예외 타입만 본다. 전달 조건 미충족은 수신자가 할 수 있는 일이 없는 상태라
 * (발신자가 조건을 설정해야 풀린다) 원문 대신 무엇을 기다리는지 알려 준다.
 */
internal fun Throwable.toDomainMessage(): UiText =
    when (this) {
        is DeliveryNotReadyException -> UiText.Resource(R.string.mindrecord_receiver_delivery_not_ready)
        else -> UiText.Resource(R.string.mindrecord_receiver_load_failed)
    }
