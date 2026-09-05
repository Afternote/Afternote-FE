package com.afternote.feature.mindrecord.domain.model

import java.time.LocalDate

data class WeeklyReport(
    val dailyQuestionAmount: Int,
    val diaryAmount: Int,
    val summaryText: String,
    val week: List<WeeklyReportDay>,
    val dailyQuestions: List<WeeklyReportDailyQuestion>,
    val emotions: List<WeeklyReportEmotion>,
    /** 서버가 주지 않으면 null — 상태는 [EmotionAnalysisStatus.UNKNOWN] 이 된다 (#725). */
    val emotionAnalysis: EmotionAnalysis?,
) {
    /**
     * 이번 주 기록 수 — 홈 `WeeklySummaryGrid` 의 THIS WEEK 카드 값 (#207).
     *
     * 깊은 생각은 기획에서 제거된 기능이라 세지 않는다. 서버는 `deepThoughtAmount` 를
     * 계속 내려주지만 DTO 에 선언하지 않아 그대로 무시된다.
     */
    val totalRecordAmount: Int
        get() = dailyQuestionAmount + diaryAmount
}

/**
 * 그 주 감정 분석 진행 상태.
 *
 * [WeeklyReport.emotions] 는 **분석 성공분만** 담기므로 빈 목록 하나로는 아래 셋을
 * 구분할 수 없다. 그 판단을 [status] 한 곳에 모아 둔다 (#725).
 */
data class EmotionAnalysis(
    val total: Int,
    val succeeded: Int,
    val pending: Int,
    val failed: Int,
) {
    val status: EmotionAnalysisStatus
        get() =
            when {
                total == 0 -> EmotionAnalysisStatus.NOTHING_TO_ANALYZE

                pending > 0 -> EmotionAnalysisStatus.PENDING

                // 일부만 실패했어도 성공분이 있으면 그 키워드를 보여주는 편이 낫다.
                // 아무것도 건지지 못한 경우만 실패로 본다.
                succeeded == 0 && failed > 0 -> EmotionAnalysisStatus.FAILED

                else -> EmotionAnalysisStatus.COMPLETED
            }
}

enum class EmotionAnalysisStatus {
    /** 그 주에 분석할 기록 자체가 없다 — 키워드 0건이 정상이다. */
    NOTHING_TO_ANALYZE,

    /** 아직 분석 중이다 — 키워드 0건을 확정하면 안 된다. */
    PENDING,

    /** 재시도까지 소진해 아무것도 분석하지 못했다 — 재조회 경로가 필요하다. */
    FAILED,

    /** 분석이 끝났다. 키워드가 0건이면 실제로 0건인 것이다. */
    COMPLETED,

    /**
     * 서버가 진행 상태를 주지 않았다 — 0건인지 분석 중인지 알 수 없다.
     *
     * 계약상 오지 않을 자리지만, 그렇다고 화면 전체를 오류로 만들 이유는 없다. 확정만
     * 하지 않으면 되므로 카드 한 장이 «모른다» 를 표시하고 나머지는 그대로 그린다.
     */
    UNKNOWN,
}

data class WeeklyReportDay(
    val diaryId: Long,
    val day: Int,
    val isDiary: Boolean,
    /**
     * 기록일수에 세는 종류인지 (#590).
     *
     * `week[]` 는 일기 외의 종류도 싣는다. 데일리질문은 세지만 **깊은 생각은 기획에서
     * 제거된 기능이라 세지 않는다** — 서버가 계속 내려줘도 무시한다.
     */
    val countsAsRecord: Boolean,
    val emotion: TodayMood?,
)

data class WeeklyReportDailyQuestion(
    val title: String,
    val content: String,
    /**
     * 답변 날짜. **와이어 문자열이 아니라 해석된 값**이다 (#547).
     *
     * 서버 포맷(`yyyy.MM.dd 요일` / ISO) 해석은 data 계층 mapper 가 맡고, 해석하지 못한
     * 항목은 그 경계에서 제외된다. 그래서 여기까지 올라온 값은 언제나 유효하고,
     * 소비처마다 파싱을 다시 구현하거나 실패 폴백을 따로 정할 필요가 없다.
     */
    val date: LocalDate,
)

data class WeeklyReportEmotion(
    val keyword: String,
    val percentage: Int,
)
