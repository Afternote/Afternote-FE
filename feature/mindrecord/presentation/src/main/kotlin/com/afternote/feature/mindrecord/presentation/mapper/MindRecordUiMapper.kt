package com.afternote.feature.mindrecord.presentation.mapper

import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.afternote.feature.mindrecord.domain.model.DailyQuestion as DailyQuestionDomain

// 서버는 보통 `"yyyy.MM.dd 요일"` (예: "2026.05.22 금") 형식으로 내려주고,
// 일부 경로는 ISO 날짜(`2026-03-21`) · ISO 날짜시각(`2026-03-21T20:13:42`) 도 온다.
// ISO_DATE 만 두면 시각이 붙은 값에서 뒤가 남아 파싱이 실패하고, 그대로 오늘 날짜로
// 폴백해 모든 기록이 같은 날에 찍힌다 — 세 포맷을 모두 허용한다.
private val DateFormatters: List<DateTimeFormatter> =
    listOf(
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ISO_DATE,
        DateTimeFormatter.ISO_DATE_TIME,
    )

fun DailyQuestionDomain.toUi(): DailyQuestion =
    DailyQuestion(
        id = dailyQuestionId,
        title = title,
        date = parseLocalDate(createdAt),
        content = content,
        imageUrl = imageUrl,
    )

/**
 * 날짜를 못 정하면 `null` 을 돌린다 — 호출부가 그 항목을 목록에서 뺀다.
 *
 * 오늘 날짜로 폴백하지 않는다. 이 매퍼의 두 날짜 후보가 모두 비는 것은 서버 응답에
 * 필드가 없었다는 뜻인데, 그걸 오늘로 메우면 날짜 없는 항목이 오늘 칸에 정상 기록처럼
 * 앉아 필드 누락을 감춘다.
 */
fun Diary.toUi(): DailyDiary? {
    // 캘린더에 찍히는 값은 사용자가 고른 일기 날짜(`date`)다. `createdAt`(레코드 생성
    // 시각)을 쓰면 지난 날짜로 쓴 일기가 작성한 날에 찍힌다 — 서버가 `date` 를 주지
    // 않을 때만 폴백한다.
    val resolvedDate = parseLocalDateOrNull(date) ?: parseLocalDateOrNull(createdAt) ?: return null
    return DailyDiary(
        id = diaryId,
        title = title,
        date = resolvedDate,
        content = content,
        emotion = todayMood?.toEmoji(),
        imageUrl = imageUrl,
    )
}

fun TodayMood.toEmoji(): String =
    when (this) {
        TodayMood.HAPPY -> "😊"
        TodayMood.SOSO -> "😐"
        TodayMood.SAD -> "😢"
    }

private fun parseLocalDate(raw: String): LocalDate = parseLocalDateOrNull(raw) ?: LocalDate.now()

private fun parseLocalDateOrNull(raw: String?): LocalDate? {
    // "2026.05.22 금" 처럼 뒤에 요일이 붙어오는 케이스 대응 — 첫 공백 앞부분만 파싱.
    val datePart = raw?.substringBefore(' ')?.trim().orEmpty()
    if (datePart.isEmpty()) return null
    for (formatter in DateFormatters) {
        runCatching { return LocalDate.parse(datePart, formatter) }
    }
    return null
}
