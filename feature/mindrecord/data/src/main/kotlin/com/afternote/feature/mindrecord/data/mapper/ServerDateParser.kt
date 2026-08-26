package com.afternote.feature.mindrecord.data.mapper

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 서버가 내려주는 날짜 문자열을 [LocalDate] 로 해석한다 (해석 못 하면 null).
 *
 * 응답 포맷 지식을 **DTO→도메인 경계에 둔다.** 종전에는 `WeeklyReportViewModel` 이
 * "서버가 `yyyy.MM.dd 요일` 또는 ISO 로 내려준다" 는 사실을 알고 직접 파싱했다 —
 * presentation 이 와이어 포맷을 아는 구조라, 도메인 소비자가 늘 때마다 같은 파싱을
 * 다시 구현해야 했다 (#547).
 *
 * 세 포맷을 모두 허용한다. `ISO_DATE` 만 두면 시각이 붙은 값(`2026-03-21T20:13:42`)에서
 * 뒤가 남아 파싱이 실패한다. 요일이 붙은 값(`2026.05.22 금`)은 앞 조각만 쓴다.
 */
internal fun parseServerDateOrNull(raw: String): LocalDate? {
    val datePart = raw.substringBefore(' ').trim()
    if (datePart.isEmpty()) return null
    for (formatter in SERVER_DATE_FORMATTERS) {
        val parsed = runCatching { LocalDate.parse(datePart, formatter) }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

private val SERVER_DATE_FORMATTERS: List<DateTimeFormatter> =
    listOf(
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ISO_DATE,
        DateTimeFormatter.ISO_DATE_TIME,
    )
