package com.afternote.core.model

data class HomeSummary(
    val userName: String,
    val isRecipientDesignated: Boolean,
    val diaryCategoryCount: Int,
    /**
     * 오늘의 질문 **본문 문자열**. 조회 실패 시 null — UI 는 중립 문구를 표시한다.
     *
     * 질문 개체(`TodayDailyQuestion`)가 아니라 `content` 하나만 담는다는 뜻에서 Content 를 붙였다.
     */
    val todayQuestionContent: String?,
)
