package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.error.DeliveryNotReadyException
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
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
    ) : ViewModel() {
        private val rawRecords = MutableStateFlow<ReceiverMindRecords?>(null)
        private val _uiState = MutableStateFlow<ReceiverMindRecordUiState>(ReceiverMindRecordUiState.Loading)
        val uiState: StateFlow<ReceiverMindRecordUiState> = _uiState.asStateFlow()

        init {
            load()
        }

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

        private fun load() {
            viewModelScope.launch {
                _uiState.update { ReceiverMindRecordUiState.Loading }
                repository
                    .getAll()
                    .onSuccess { records ->
                        rawRecords.value = records
                        _uiState.value =
                            ReceiverMindRecordUiState
                                .Success(
                                    dailyQuestions = emptyList(),
                                    diaries = emptyList(),
                                ).withDerived()
                    }.onFailure { e ->
                        _uiState.value =
                            ReceiverMindRecordUiState.Error(message = e.toDomainMessage())
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
