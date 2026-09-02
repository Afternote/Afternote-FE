package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.util.RecordContentBlock
import java.time.LocalDate

/**
 * 기록 상세 화면 상태 (#759, Figma 3814:18721 외 3종).
 *
 * 데일리질문과 일기가 **같은 화면**을 쓴다. 시안 4종의 차이는 두 축뿐이다 —
 * 첨부 이미지 유무([heroImageUrl])와 기록 종류([mood] 유무).
 */
sealed interface RecordDetailUiState {
    data object Loading : RecordDetailUiState

    data class Success(
        val title: String,
        /**
         * 기록 날짜. **없으면 null 이고 그 줄을 그리지 않는다** — 지어낸 날짜를 그리면
         * 사용자는 그 기록이 실제로 그 날 쓰인 것으로 읽는다. 상세에서 날짜는 부수
         * 정보가 아니라 기록을 식별하는 값이다 (#759 리뷰).
         */
        val date: LocalDate?,
        /** "수신인 OOO" 로 보여줄 이름들. 지정하지 않았으면 빈 목록이라 줄 자체를 그리지 않는다. */
        val receiverNames: List<String>,
        /**
         * 헤더 배경에 깔 이미지. null 이면 시안의 "이미지 X" 변형 —
         * 사진 대신 옅은 그라데이션을 깔고 글자색이 밝은색에서 어두운색으로 뒤집힌다.
         */
        val heroImageUrl: String?,
        val blocks: List<RecordContentBlock>,
        /** 일기에만 있는 오늘의 기분 이모지. 데일리질문이면 null 이라 그 줄이 없다. */
        val mood: String?,
    ) : RecordDetailUiState

    data class Error(
        val message: UiText,
    ) : RecordDetailUiState

    /**
     * 목록에 그 기록이 없다 — 지워졌거나 다른 달에 있다.
     *
     * 통신 실패와 구분한다. 사용자가 할 일이 다르기 때문이다 — 통신 실패는 «다시 시도»,
     * 없는 기록은 «목록으로». 종전에는 둘이 같은 화면으로 수렴해 재시도를 아무리 눌러도
     * 같은 결과였다 (#759 리뷰).
     */
    data object NotFound : RecordDetailUiState
}
