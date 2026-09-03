package com.afternote.feature.receiver.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.receiver.data.reporting.RECEIVER_LIST_DECODING_STAGE
import com.afternote.feature.receiver.data.reporting.RECEIVER_LIST_MAPPING_STAGE
import com.afternote.feature.receiver.data.reporting.RecordingErrorReporter
import com.afternote.feature.receiver.data.reporting.assertReceiverListFailureContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ReceivedAfternoteListDto.toReceiverDomainList] / [ReceivedAfternoteListDto.toDomainResult] 회귀 가드.
 * 수신자 목록 DTO→[com.afternote.feature.receiver.domain.model.AfterNoteListItem] 매핑.
 * 서버 카테고리를 [AfternoteType] 으로 바꾸는 규칙과 createdAt null 가드를 검증한다.
 *
 * 항목 하나를 옮기는 변환과 제외 집계는 이 파일 안에 갇힌 `private` 헬퍼다 (#1832). 그래서
 * 목록을 받는 공개 owner 로만 넣고, 나온 목록과 텔레메트리 속성으로 같은 계약을 확인한다.
 */
class ReceiverAfternoteListItemDtoToDomainTest {
    @Test
    fun `목록 매핑 - 필드 매핑 + category 정규화 + 날짜 포맷`() {
        val result =
            mapItems(
                resp(id = 9L, title = "사진첩", category = "GALLERY", createdAt = "2025-11-26T14:30:00"),
            ).single()

        assertEquals(9L, result.id)
        assertEquals("사진첩", result.serviceName)
        assertEquals(AfternoteType.GALLERY_AND_FILES, result.type)
        assertEquals("2025.11.26", result.lastUpdatedAt)
    }

    @Test
    fun `목록 매핑 - 변환은 대소문자 무시 + MUSIC PLAYLIST는 MEMORIAL`() {
        assertEquals(AfternoteType.SOCIAL_NETWORK, mapItems(resp(category = "social")).single().type)
        assertEquals(AfternoteType.MEMORIAL, mapItems(resp(category = "PLAYLIST")).single().type)
        assertEquals(AfternoteType.MEMORIAL, mapItems(resp(category = "music")).single().type)
    }

    @Test
    fun `목록 매핑 - 사업자는 서버가 아는 종류다 - BUSINESS 로 올라온다`() {
        assertEquals(AfternoteType.BUSINESS, mapItems(resp(category = "BUSINESS")).single().type)
    }

    @Test
    fun `목록 매핑 - 서버가 모르는 category 를 보내면 항목을 거절한다`() {
        assertTrue(mapItems(resp(category = "ESTATE")).isEmpty())
    }

    @Test
    fun `목록 매핑 - 아예 모르는 값도 항목을 거절하고 특정 종류로 메우지 않는다`() {
        assertTrue(mapItems(resp(category = "WHAT_IS_THIS")).isEmpty())
    }

    @Test
    fun `목록 매핑 - createdAt null이면 lastUpdatedAt null`() {
        assertNull(mapItems(resp(createdAt = null)).single().lastUpdatedAt)
    }

    @Test
    fun `toReceiverDomainList - 각 항목을 순서대로 매핑`() {
        val reporter = RecordingErrorReporter()

        val list = listDto(resp(id = 1L), resp(id = 2L)).toReceiverDomainList(reporter)

        assertEquals(listOf(1L, 2L), list.map { it.id })
        assertEquals(0, reporter.failures.size)
    }

    @Test
    fun `toReceiverDomainList - 지원하지 않는 category 항목만 제외하고 유효 항목은 보존한다`() {
        val reporter = RecordingErrorReporter()
        val list =
            listDto(
                resp(id = 1L, category = "SOCIAL"),
                resp(id = 2L, category = "ESTATE"),
                resp(id = 3L, category = "BUSINESS"),
            ).toReceiverDomainList(reporter)

        assertEquals(listOf(1L, 3L), list.map { it.id })
        reporter.assertReceiverListFailureContract(mapOf(RECEIVER_LIST_MAPPING_STAGE to "1"))
    }

    @Test
    fun `toDomainResult - 서버 totalCount와 목록을 함께 보존`() {
        val reporter = RecordingErrorReporter()
        val result =
            ReceivedAfternoteListDto(
                afternotes = listOf(resp(id = 3L), resp(id = 4L)),
                totalCount = 7,
            ).toDomainResult(reporter)

        assertEquals(7, result.totalCount)
        assertEquals(listOf(3L, 4L), result.items.map { it.id })
        assertEquals(0, reporter.failures.size)
    }

    @Test
    fun `toDomainResult - 디코딩 실패와 category 매핑 실패를 별도 이벤트로 보고한다`() {
        val reporter = RecordingErrorReporter()
        val result =
            ReceivedAfternoteListDto(
                afternotes =
                    listOf(
                        resp(id = 1L, category = "SOCIAL"),
                        resp(id = 2L, category = "ESTATE"),
                        resp(id = 3L, category = "BUSINESS"),
                    ),
                totalCount = 7,
                decodingRejectedItemCount = 2,
            ).toDomainResult(reporter)

        assertEquals(7, result.totalCount)
        assertEquals(listOf(1L, 3L), result.items.map { it.id })

        reporter.assertReceiverListFailureContract(
            mapOf(
                RECEIVER_LIST_DECODING_STAGE to "2",
                RECEIVER_LIST_MAPPING_STAGE to "1",
            ),
        )
    }

    /** 제외가 없는 정상 경로만 쓰는 헬퍼 — 텔레메트리를 보는 케이스는 reporter 를 직접 들고 단언한다. */
    private fun mapItems(vararg dtos: ReceivedAfternoteDto) = listDto(*dtos).toReceiverDomainList(RecordingErrorReporter())

    private fun listDto(vararg dtos: ReceivedAfternoteDto) =
        ReceivedAfternoteListDto(
            afternotes = dtos.toList(),
            totalCount = dtos.size,
        )

    private fun resp(
        id: Long = 1L,
        title: String = "t",
        category: String = "SOCIAL",
        createdAt: String? = "2025-01-01T00:00:00",
    ) = ReceivedAfternoteDto(
        id = id,
        title = title,
        category = category,
        createdAt = createdAt,
    )
}
