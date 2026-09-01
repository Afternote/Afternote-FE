package com.afternote.feature.receiver.data.mapper

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.receiver.data.reporting.RecordingErrorReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [ReceivedAfternoteDto.toDomainOrNull] / [toReceiverDomainList] 회귀 가드.
 * 수신자 목록 DTO→[com.afternote.feature.receiver.domain.model.AfterNoteListItem] 매핑.
 * 서버 카테고리를 [AfternoteType] 으로 바꾸는 규칙과 createdAt null 가드를 검증한다.
 */
class ReceiverAfternoteListItemDtoToDomainTest {
    @Test
    fun `toDomainOrNull - 필드 매핑 + category 정규화 + 날짜 포맷`() {
        val result =
            ReceivedAfternoteDto(
                id = 9L,
                title = "사진첩",
                category = "GALLERY",
                createdAt = "2025-11-26T14:30:00",
            ).toDomainOrNull()

        requireNotNull(result)

        assertEquals(9L, result.id)
        assertEquals("사진첩", result.serviceName)
        assertEquals(AfternoteType.GALLERY_AND_FILES, result.type)
        assertEquals("2025.11.26", result.lastUpdatedAt)
    }

    @Test
    fun `toDomainOrNull - 변환은 대소문자 무시 + MUSIC PLAYLIST는 MEMORIAL`() {
        assertEquals(AfternoteType.SOCIAL_NETWORK, requireNotNull(resp(category = "social").toDomainOrNull()).type)
        assertEquals(AfternoteType.MEMORIAL, requireNotNull(resp(category = "PLAYLIST").toDomainOrNull()).type)
        assertEquals(AfternoteType.MEMORIAL, requireNotNull(resp(category = "music").toDomainOrNull()).type)
    }

    @Test
    fun `toDomainOrNull - 사업자는 서버가 아는 종류다 - BUSINESS 로 올라온다`() {
        assertEquals(AfternoteType.BUSINESS, requireNotNull(resp(category = "BUSINESS").toDomainOrNull()).type)
    }

    @Test
    fun `toDomainOrNull - 서버가 모르는 category 를 보내면 항목을 거절한다`() {
        assertNull(resp(category = "ESTATE").toDomainOrNull())
    }

    @Test
    fun `toDomainOrNull - 아예 모르는 값도 항목을 거절하고 특정 종류로 메우지 않는다`() {
        assertNull(resp(category = "WHAT_IS_THIS").toDomainOrNull())
    }

    @Test
    fun `toDomainOrNull - createdAt null이면 lastUpdatedAt null`() {
        assertNull(requireNotNull(resp(createdAt = null).toDomainOrNull()).lastUpdatedAt)
    }

    @Test
    fun `toReceiverDomainList - 각 항목을 순서대로 매핑`() {
        val reporter = RecordingErrorReporter()

        val list = listOf(resp(id = 1L), resp(id = 2L)).toReceiverDomainList(reporter)

        assertEquals(listOf(1L, 2L), list.map { it.id })
        assertEquals(0, reporter.failures.size)
    }

    @Test
    fun `toReceiverDomainList - 지원하지 않는 category 항목만 제외하고 유효 항목은 보존한다`() {
        val reporter = RecordingErrorReporter()
        val list =
            listOf(
                resp(id = 1L, category = "SOCIAL"),
                resp(id = 2L, category = "ESTATE"),
                resp(id = 3L, category = "BUSINESS"),
            ).toReceiverDomainList(reporter)

        assertEquals(listOf(1L, 3L), list.map { it.id })
        assertEquals(1, reporter.failures.size)
        assertEquals("receiver_list_mapping", reporter.failures.single().attributes["receiver_stage"])
        assertEquals("1", reporter.failures.single().attributes["rejected_item_count"])
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

        val failuresByStage = reporter.failures.associateBy { it.attributes["receiver_stage"] }
        assertEquals(setOf("receiver_list_decoding", "receiver_list_mapping"), failuresByStage.keys)

        val decodingFailure = requireNotNull(failuresByStage["receiver_list_decoding"])
        assertEquals("2", decodingFailure.attributes["rejected_item_count"])
        assertEquals(ReceiverListDecodingFailure::class.java.name, decodingFailure.attributes["error_type"])

        val mappingFailure = requireNotNull(failuresByStage["receiver_list_mapping"])
        assertEquals("1", mappingFailure.attributes["rejected_item_count"])
        assertEquals(ReceiverListMappingFailure::class.java.name, mappingFailure.attributes["error_type"])
    }

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
