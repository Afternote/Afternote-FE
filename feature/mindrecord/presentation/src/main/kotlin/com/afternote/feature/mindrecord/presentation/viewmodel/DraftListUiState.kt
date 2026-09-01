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
        /** 직전 삭제 요청의 결과. `null` 이면 알릴 것이 없다. */
        val deleteOutcome: DraftDeleteOutcome? = null,
    ) : DraftListUiState {
        val totalCount: Int get() = items.size
    }

    data class Error(
        val message: UiText,
    ) : DraftListUiState
}

/**
 * 삭제 요청의 결과.
 *
 * 항목별 `delete()` 는 실패를 던지지 않고 `Result.failure` 로 감싼다. 그 결과를 보지 않으면
 * 서버가 거절한 항목이 목록에 남은 채 완료 토스트가 떠서, 사용자는 지워진 줄 안다 (#442).
 */
sealed interface DraftDeleteOutcome {
    /** 요청한 항목이 모두 지워졌다. */
    data object AllDeleted : DraftDeleteOutcome

    /** 일부가 실패했다. 실패한 항목은 화면이 다시 선택 상태로 되돌려 재시도할 수 있게 한다. */
    data class SomeFailed(
        val failedItems: List<DraftItem>,
    ) : DraftDeleteOutcome
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
