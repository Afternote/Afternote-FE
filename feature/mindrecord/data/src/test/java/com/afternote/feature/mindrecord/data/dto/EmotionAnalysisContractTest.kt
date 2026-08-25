package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.di.NetworkModule
import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.mapper.toDomain
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysisStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 감정 분석 상태 계약 가드 (#725, OpenAPI `EmotionAnalysisSummary`).
 *
 * `emotions` 는 **분석 성공분만** 담기므로, 빈 배열 하나로는 "분석이 끝났는데 키워드가
 * 없음" 과 "아직 분석 중" 과 "분석 실패" 가 구분되지 않는다. 그 셋을 가르는 값이
 * `emotionAnalysis` 라, 이 필드가 소실되면 이슈가 고치려는 오표시가 그대로 재현된다.
 *
 * Json 은 프로덕션 설정을 **복제하지 않고 그대로 가져다 쓴다** — 복제하면 앱 설정이
 * 바뀔 때 이 테스트만 조용히 어긋난다.
 */
@OptIn(ExperimentalSerializationApi::class)
class EmotionAnalysisContractTest {
    private val json = NetworkModule.provideJson()

    private fun decodeStatus(analysisJson: String): EmotionAnalysisStatus? {
        val body =
            """
            { "status": 200, "code": 200, "data": {
              "dailyQuestionAmount": 0, "diaryAmount": 0, "summaryText": "",
              "daily-question": [], "week": [], "emotions": [],
              "emotionAnalysis": $analysisJson } }
            """.trimIndent()
        return json
            .decodeFromString(BaseResponse.serializer(WeeklyReportDto.serializer()), body)
            .data!!
            .toDomain()
            .emotionAnalysis
            ?.status
    }

    @Test
    fun `분석 대상이 없으면 키워드 0건이 정상이다`() {
        val status = decodeStatus("""{ "total": 0, "succeeded": 0, "pending": 0, "failed": 0 }""")

        assertEquals(EmotionAnalysisStatus.NOTHING_TO_ANALYZE, status)
    }

    @Test
    fun `대기 중인 기록이 하나라도 있으면 대기다`() {
        // 실서버 실측(2026-08-23) — 일기 저장 직후 1분 넘게 이 상태가 유지됐다.
        // 이때 emotions 는 [] 라, 이 값이 없으면 앱이 "키워드 0건" 으로 확정해 버린다.
        val status = decodeStatus("""{ "total": 1, "succeeded": 0, "pending": 1, "failed": 0 }""")

        assertEquals(EmotionAnalysisStatus.PENDING, status)
    }

    @Test
    fun `성공분이 있어도 대기가 남아 있으면 대기다`() {
        // 부분 성공 중간 상태 — 아직 키워드가 더 붙을 수 있으므로 확정하지 않는다.
        val status = decodeStatus("""{ "total": 2, "succeeded": 1, "pending": 1, "failed": 0 }""")

        assertEquals(EmotionAnalysisStatus.PENDING, status)
    }

    @Test
    fun `재시도까지 소진해 아무것도 못 건지면 실패다`() {
        val status = decodeStatus("""{ "total": 2, "succeeded": 0, "pending": 0, "failed": 2 }""")

        assertEquals(EmotionAnalysisStatus.FAILED, status)
    }

    @Test
    fun `일부만 실패했고 성공분이 있으면 완료로 본다`() {
        // 건진 키워드를 보여주는 편이 낫다 — 실패 화면으로 덮으면 있는 결과까지 가린다.
        val status = decodeStatus("""{ "total": 3, "succeeded": 2, "pending": 0, "failed": 1 }""")

        assertEquals(EmotionAnalysisStatus.COMPLETED, status)
    }

    @Test
    fun `전부 성공하면 완료다`() {
        val status = decodeStatus("""{ "total": 3, "succeeded": 3, "pending": 0, "failed": 0 }""")

        assertEquals(EmotionAnalysisStatus.COMPLETED, status)
    }

    @Test
    fun `emotionAnalysis 키가 빠져도 0 건으로 접히지 않는다`() {
        // 보정형 기본값을 두면 "분석 대기" 가 "분석할 것 없음" 으로 접혀 이 이슈가 재현된다.
        // 그렇다고 필수로 두면 이 필드 하나에 주간리포트 탭 전체가 오류 화면이 되므로,
        // null 로 받아 «모른다» 로 옮긴다 — 확정하지 않는다는 목적은 그대로다.
        val body =
            """
            { "status": 200, "code": 200, "data": {
              "dailyQuestionAmount": 0, "diaryAmount": 0, "summaryText": "",
              "daily-question": [], "week": [], "emotions": [] } }
            """.trimIndent()

        val report =
            json
                .decodeFromString(BaseResponse.serializer(WeeklyReportDto.serializer()), body)
                .data!!
                .toDomain()

        assertNull(report.emotionAnalysis)
    }

    @Test
    fun `나머지 필드가 성해도 감정 분석만 빠지면 화면 전체가 실패하지 않는다`() {
        // 필수로 두던 시절에는 여기서 MissingFieldException 이 나 탭 전체가 ErrorBox 였고,
        // UiText.DynamicOrResource 가 e.message 를 우선해 영문 예외 원문까지 노출됐다.
        val body =
            """
            { "status": 200, "code": 200, "data": {
              "dailyQuestionAmount": 2, "diaryAmount": 1, "summaryText": "이번 주 요약",
              "daily-question": [], "week": [], "emotions": [] } }
            """.trimIndent()

        val report =
            json
                .decodeFromString(BaseResponse.serializer(WeeklyReportDto.serializer()), body)
                .data!!
                .toDomain()

        assertEquals("이번 주 요약", report.summaryText)
        assertEquals(2, report.dailyQuestionAmount)
    }
}
