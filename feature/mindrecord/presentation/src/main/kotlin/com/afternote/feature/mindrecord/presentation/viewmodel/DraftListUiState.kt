package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import java.time.LocalDate

/**
 * 작성 화면 키보드 툴바 "임시저장 N" 영역에서 진입하는 임시저장 목록 화면 상태.
 *
 * 데일리질문 / 일기 / 깊은 생각 3개 카테고리의 isDraft=true 항목을 합쳐 보여준다.
 * 데일리질문 응답은 isDraft 필드를 노출하지 않아 현재는 분류 불가 — 화면 필터 옵션은 유지하되
 * 실제 항목은 비어 있게 보인다 (TODO: 백엔드 응답 확장 후 매핑).
 */
sealed interface DraftListUiState {
    data object Loading : DraftListUiState

    data class Success(
        val items: List<DraftItem>,
        val filter: DraftCategory,
    ) : DraftListUiState {
        val filtered: List<DraftItem>
            get() =
                when (filter) {
                    DraftCategory.All -> items
                    else -> items.filter { it.category == filter }
                }

        val totalCount: Int get() = filtered.size
    }

    data class Error(
        val message: UiText,
    ) : DraftListUiState
}

enum class DraftCategory {
    All,
    DailyQuestion,
    Diary,
    DeepThought,
}

data class DraftItem(
    val id: Long,
    val category: DraftCategory,
    val content: String,
    val date: LocalDate,
)
