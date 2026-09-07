package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryRecord
import com.afternote.feature.mindrecord.presentation.reporting.MindRecordFailureStage
import com.afternote.feature.mindrecord.presentation.reporting.recordMindRecordFailure
import com.afternote.feature.mindrecord.presentation.usecase.GetMemorySpaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 추억 공간(MEMORY SPACE) 화면.
 *
 * 서버에 "추억" 전용 계약이 없어 사용자의 실제 기록을 모아 카드로 만든다. **무엇을 모으고
 * 무엇을 버리는지는 [GetMemorySpaceUseCase] 가 안다** (#1693) — 비대칭 조회 범위, 부분 실패,
 * 초안 제외, 정렬과 상한이 그쪽 정책이다. 여기 남는 것은 화면 상태와 카드 문구, 그리고
 * 실패 승격뿐이다.
 */
@HiltViewModel
class MemorySpaceViewModel
    @Inject
    constructor(
        private val getMemorySpace: GetMemorySpaceUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<MemorySpaceUiState>(MemorySpaceUiState.Loading)
        val uiState: StateFlow<MemorySpaceUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun retry() = load()

        private fun load() {
            viewModelScope.launch {
                _uiState.value = MemorySpaceUiState.Loading
                getMemorySpace
                    .invoke()
                    .onSuccess { records ->
                        _uiState.value = MemorySpaceUiState.Success(records.map { it.toCard() })
                    }.onFailure { throwable ->
                        // 부분 실패는 UseCase 가 삼키므로, 여기 오는 것은 **합친 결과가 비었고
                        // 실패 출처가 하나라도 있을 때**다 — 화면이 통째로 비는 자리라 승격
                        // 가치가 높다. 출처는 넷이다: 일기 최근 3개월이 달마다 하나씩,
                        // 데일리질문이 하나 (#964 리뷰).
                        errorReporter.recordMindRecordFailure(MindRecordFailureStage.MEMORY_SPACE_LOAD, throwable)
                        _uiState.value = MemorySpaceUiState.Error(R.string.mindrecord_error_memory_space_failed)
                    }
            }
        }

        /** 카드 문구는 화면 몫이다 — 표시용 날짜 서식과 태그 자리를 여기서 만든다. */
        private fun MemoryRecord.toCard(): MemoryItem =
            MemoryItem(
                id = id,
                imageUrl = imageUrl,
                title = title,
                date = date.format(CARD_DATE_FORMATTER),
                content = content,
                tags = listOfNotNull(emotion),
            )

        private companion object {
            val CARD_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        }
    }
