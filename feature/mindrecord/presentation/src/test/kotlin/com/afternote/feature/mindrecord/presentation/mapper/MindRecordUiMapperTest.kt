package com.afternote.feature.mindrecord.presentation.mapper

import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import com.afternote.feature.mindrecord.domain.model.DailyQuestion as DailyQuestionDomain

/**
 * [Diary.toUi] 날짜 해석 계약 가드 (#699).
 *
 * 캘린더에 찍히는 값은 사용자가 고른 `date` 이고 `createdAt`(생성 시각)은 폴백이다.
 * 둘 다 없으면 오늘로 메우지 않고 항목을 뺀다 — 메우면 날짜 없는 항목이 오늘 칸에
 * 정상 기록처럼 앉아 서버 필드 누락을 감춘다.
 */
class MindRecordUiMapperTest {
    @Test
    fun `date 가 있으면 createdAt 이 아니라 date 를 쓴다`() {
        val ui = diary(date = "2026-03-21", createdAt = "2026-03-25T20:13:42").toUi()

        assertEquals(LocalDate.of(2026, 3, 21), ui?.date)
    }

    @Test
    fun `date 가 없으면 createdAt 으로 폴백한다`() {
        val ui = diary(date = "", createdAt = "2026.03.25 수").toUi()

        assertEquals(LocalDate.of(2026, 3, 25), ui?.date)
    }

    @Test
    fun `ISO 날짜시각 createdAt 도 파싱된다`() {
        // Swagger DiaryResponse 예시가 `createdAt: "2026-03-25T20:13:42"` 형태다.
        val ui = diary(date = "", createdAt = "2026-03-25T20:13:42").toUi()

        assertEquals(LocalDate.of(2026, 3, 25), ui?.date)
    }

    @Test
    fun `두 날짜가 모두 없으면 오늘로 메우지 않고 항목을 뺀다`() {
        // #789 이후 두 필드 모두 계약이라 빈 응답은 파싱 단계에서 걸러지지만, 표시 단계가
        // 여전히 날짜 없는 항목을 오늘로 메우지 않는다는 것은 그대로 고정해 둔다.
        assertNull(diary(date = "", createdAt = "").toUi())
    }

    @Test
    fun `해석할 수 없는 날짜 문자열도 항목을 뺀다`() {
        assertNull(diary(date = "알 수 없음", createdAt = "").toUi())
    }

    private fun diary(
        date: String,
        createdAt: String,
    ) = Diary(
        diaryId = 1L,
        title = "제목",
        content = "본문",
        date = date,
        createdAt = createdAt,
        todayMood = TodayMood.HAPPY,
    )

    @Test
    fun `일기 썸네일도 본문 HTML 의 첫 이미지에서 나온다`() {
        // 서버 계약에 imageUrl 이 없다 — 요청에 실어 보내도 버려지고 응답에도 키가 없어
        // 항상 null 이다. 그대로 두면 일기 카드 썸네일 자리가 영영 빈다 (#1024).
        val diary =
            Diary(
                diaryId = 1L,
                title = "제목",
                content = "<p>본문</p><img src=\"https://cdn.example/mindrecords/permanent/13/a.png\" />",
                date = "2026-08-25",
                createdAt = "2026.08.25 화",
                todayMood = TodayMood.HAPPY,
            )

        assertEquals("https://cdn.example/mindrecords/permanent/13/a.png", diary.toUi()?.imageUrl)
    }

    @Test
    fun `본문에 이미지가 없는 일기는 썸네일 없이 텍스트 카드로 간다`() {
        val diary =
            Diary(
                diaryId = 2L,
                title = "제목",
                content = "<p>글만 있는 본문</p>",
                date = "2026-08-25",
                createdAt = "2026.08.25 화",
                todayMood = TodayMood.SOSO,
            )

        assertNull(diary.toUi()?.imageUrl)
    }

    @Test
    fun `데일리질문 썸네일은 본문 HTML 의 첫 이미지에서 나온다`() {
        // 서버 계약에 `imageUrl` 이 없어 응답 필드로는 영영 null 이다 — 본문에서 뽑지 않으면
        // 이미지를 첨부해도 목록 카드 썸네일이 뜨지 않는다 (#549).
        val ui =
            dailyQuestion(content = "<p>사진과 함께</p><img src=\"https://cdn/a.png\" />").toUi()!!

        assertEquals("https://cdn/a.png", ui.imageUrl)
    }

    @Test
    fun `본문에 이미지가 없으면 썸네일 없이 텍스트 카드로 간다`() {
        val ui = dailyQuestion(content = "<p>글만 있는 답변</p>").toUi()!!

        assertNull(ui.imageUrl)
    }

    private fun dailyQuestion(content: String) =
        DailyQuestionDomain(
            dailyQuestionId = 1L,
            title = "오늘의 질문",
            content = content,
            createdAt = "2026.08.23 일",
        )
}
