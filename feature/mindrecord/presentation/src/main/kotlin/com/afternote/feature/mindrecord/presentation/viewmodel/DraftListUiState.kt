package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import java.time.LocalDate

/**
 * 작성 화면 키보드 툴바 "임시저장 N" 영역에서 진입하는 임시저장 목록 화면 상태.
 *
 * 데일리질문 / 일기 / 깊은 생각 3개 카테고리의 isDraft=true 항목을 합쳐 보여준다.
 * 데일리질문 응답은 isDraft 필드를 노출하지 않아 현재는 분류 불가 — 실제 항목은 비어 있게 보인다
 * (TODO: 백엔드 응답 확장 후 매핑).
 *
 * Figma 2372:22842 리디자인 — 카테고리 필터가 제거되고 선택(다중 선택 삭제) 모드가 추가됐다.
 */
sealed interface DraftListUiState {
    data object Loading : DraftListUiState

    data class Success(
        val items: List<DraftItem>,
        val selectionMode: Boolean = false,
        val selectedKeys: Set<String> = emptySet(),
    ) : DraftListUiState {
        val totalCount: Int get() = items.size

        val selectedCount: Int get() = selectedKeys.size
    }

    data class Error(
        val message: UiText,
    ) : DraftListUiState
}

enum class DraftCategory {
    DailyQuestion,
    Diary,
    DeepThought,
}

data class DraftItem(
    val id: Long,
    val category: DraftCategory,
    val content: String,
    val date: LocalDate,
    /** 수신인 이름. 백엔드 응답에 아직 없어 null이면 카테고리 라벨로 대체 표기한다 (TODO). */
    val recipientName: String? = null,
) {
    /** 카테고리별 id 충돌을 피하기 위한 리스트/선택용 고유 키. */
    val key: String get() = "${category.name}-$id"
}
