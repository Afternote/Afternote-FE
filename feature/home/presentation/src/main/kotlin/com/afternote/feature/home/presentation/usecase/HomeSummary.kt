package com.afternote.feature.home.presentation.usecase

data class HomeSummary(
    val userName: String,
    val isRecipientDesignated: Boolean,
    /**
     * 오늘의 질문 **본문 문자열**. 조회 실패 시 null — UI 는 중립 문구를 표시한다.
     *
     * 질문 개체(`TodayDailyQuestion`)가 아니라 `content` 하나만 담는다는 뜻에서 Content 를 붙였다.
     */
    val todayQuestionContent: String?,
    /**
     * 이번 주(월~일) 기록 수. 조회 실패 시 null — 0 으로 접으면 «기록이 없음» 을 확정한다 (#562).
     */
    val weeklyRecordCount: Int?,
)
