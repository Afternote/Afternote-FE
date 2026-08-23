package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
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
                            _uiState.value =
                                ReceiverMindRecordUiState
                                    .Success(
                                        dailyQuestions = emptyList(),
                                        diaries = emptyList(),
                                    ).withDerived()
                        }.onFailure { e ->
                            // 재진입 갱신 실패는 보고 있던 화면을 유지한다.
                            if (showsLoading) {
                                _uiState.value =
                                    ReceiverMindRecordUiState.Error(
                                        message = e.message ?: "마음의 기록을 불러오지 못했습니다.",
                                    )
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
