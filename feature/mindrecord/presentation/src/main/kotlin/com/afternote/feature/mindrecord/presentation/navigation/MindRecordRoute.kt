package com.afternote.feature.mindrecord.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface MindRecordRoute {
    @Serializable
    data object DailyQuestionWriteRoute : MindRecordRoute

    /**
     * 일기 작성 화면. [draftId] 가 있으면 임시저장 이어쓰기 모드 — 해당 달([draftYearMonth],
     * `yyyy-MM`)의 draft 목록에서 항목을 찾아 프리필하고, 저장 시 PATCH 로 수정한다.
     */
    @Serializable
    data class DiaryWriteRoute(
        val draftId: Long? = null,
        val draftYearMonth: String? = null,
    ) : MindRecordRoute

    /**
     * 기록 상세(열람) 화면 (#759).
     *
     * 데일리질문과 일기가 같은 화면을 쓴다 — 시안 4종의 차이는 첨부 이미지 유무와
     * 기록 종류뿐이다. [yearMonth](`yyyy-MM`)는 일기를 찾을 달이다.
     */
    @Serializable
    data class RecordDetailRoute(
        val recordId: Long,
        val isDiary: Boolean,
        val yearMonth: String? = null,
    ) : MindRecordRoute

    /** 작성 화면 키보드 툴바의 "임시저장 N" 영역에서 진입하는 임시저장 목록 화면. */
    @Serializable
    data object DraftListRoute : MindRecordRoute
}
