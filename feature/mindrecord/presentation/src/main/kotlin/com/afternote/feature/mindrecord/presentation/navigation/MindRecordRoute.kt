package com.afternote.feature.mindrecord.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface MindRecordRoute {
    /**
     * 데일리질문 작성 화면.
     *
     * [answerId] 가 있으면 그 레코드를 프리필하고 저장 시 PATCH 한다. [isDraft] 가 true 면
     * **임시저장 이어쓰기**(등록 시 `isDraft=false` 로 전환), false 면 **정식 답변 수정**이다.
     * 없으면 오늘 질문에 새로 답한다(당일 임시저장이 있으면 이어쓰기).
     *
     * 당일이 지난 임시저장은 이 인자로만 열 수 있다 — today 응답 기반 경로는 오늘 것만
     * 찾기 때문이다 (#770).
     */
    @Serializable
    data class DailyQuestionWriteRoute(
        val answerId: Long? = null,
        val isDraft: Boolean = false,
    ) : MindRecordRoute

    /**
     * 일기 작성 화면.
     *
     * [recordId] 가 있으면 그 일기를 프리필하고 저장 시 PATCH 한다. [isDraft] 가 true 면
     * 임시저장 이어쓰기(등록 시 `isDraft=false` 로 전환), false 면 **정식 기록 수정**이다.
     * [yearMonth](`yyyy-MM`) 는 해당 항목을 찾을 달이다.
     */
    @Serializable
    data class DiaryWriteRoute(
        val recordId: Long? = null,
        val yearMonth: String? = null,
        val isDraft: Boolean = true,
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
