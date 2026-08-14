package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import java.time.LocalDate

/**
 * 작성 화면 키보드 툴바 "임시저장 N" 영역에서 진입하는 임시저장 목록 화면 상태.
 *
 * 데일리질문 / 일기 2개 카테고리의 isDraft=true 항목을 합쳐 보여준다.
 * 데일리질문은 목록 응답의 draft 플래그로 클라 필터하며, 서버가 플래그를 내려주지 않으면
 * 기본값 false 로 파싱되어 해당 카테고리는 비어 보인다 (서버 계약 미검증).
 */
sealed interface DraftListUiState {
    data object Loading : DraftListUiState

    data class Success(
        val items: List<DraftItem>,
        val isDeleting: Boolean = false,
        val deleteCompleted: Boolean = false,
    ) : DraftListUiState {
        val totalCount: Int get() = items.size
    }

    data class Error(
        val message: UiText,
    ) : DraftListUiState
}

enum class DraftCategory {
    All,
    DailyQuestion,
    Diary,
}

data class DraftItem(
    val id: Long,
    val category: DraftCategory,
    val title: String,
    val content: String,
    val date: LocalDate,
)
