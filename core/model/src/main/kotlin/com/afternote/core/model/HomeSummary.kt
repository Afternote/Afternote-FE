package com.afternote.core.model

data class HomeSummary(
    val userName: String,
    val isRecipientDesignated: Boolean,
    val diaryCategoryCount: Int,
    /** 오늘의 질문 본문. 조회 실패 시 null — UI 는 기본 문구로 폴백한다. */
    val todayQuestion: String?,
)
