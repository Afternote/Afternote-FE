package com.afternote.feature.receiver.data.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import com.afternote.feature.receiver.data.reporting.RecordingErrorReporter
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `GET /receiver-auth/record-boxes` 응답 계약 가드 (#612 · #607).
 *
 * 페이로드는 BE `ReceivedRecordBoxResponse`·`ReceivedRecordBoxListResponse`(origin/main) 스키마를 그대로
 * 옮긴 것이다 — 목록이 `recordBoxes` 로 한 번 감싸여 오고, 시각 필드는 BE `LocalDateTime` 직렬화라
 * 타임존 표기가 없다.
 *
 * 프로덕션 경로(`ReceiverAuthRepositoryImpl.getReceivedRecordBoxes`)와 같은 순서로
 * Json 디코드 → `requireData()` → `toDomain()` 을 통과시킨다. Json 설정은 `NetworkModule.provideJson` 과 동일.
 */
class ReceivedRecordBoxContractTest {
    private val reporter = RecordingErrorReporter()

    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Test
    fun `승인된 칸은 승인일까지 도메인에 도달한다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"recordBoxes":[
            |{"receiverId":4,"accessCode":"59c04a15-1f4a-4b2e-9a0c-2f4e8d7b6c31","senderName":"김혜성",
            |"receiverName":"김지은","relation":"DAUGHTER","recordStatus":"STORED","viewStatus":"VIEWABLE",
            |"verificationStatus":"APPROVED","requestedAt":"2026-06-21T03:07:26","approvedAt":"2026-07-29T16:00:10"}]}}
            """.trimMargin().replace("\n", "")

        val boxes = json.decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload).requireData().toDomain(reporter)

        val box = boxes.single()
        assertEquals(4L, box.receiverId)
        assertEquals("59c04a15-1f4a-4b2e-9a0c-2f4e8d7b6c31", box.masterKey)
        assertEquals("김혜성", box.senderName)
        assertEquals(DeliveryVerificationStatus.APPROVED, box.verificationStatus)
        assertEquals("2026-06-21T03:07:26", box.requestedAt)
        // 발신자 상세의 '승인일' 이 이 값을 표시한다 (#612).
        assertEquals("2026-07-29T16:00:10", box.approvedAt)
    }

    @Test
    fun `승인 전 칸은 승인일이 비어 있다`() {
        val payload =
            """{"status":200,"code":200,"data":{"recordBoxes":[
            |{"receiverId":7,"accessCode":"c0ffee00-0000-4000-8000-000000000001","senderName":"박경민",
            |"receiverName":"김지은","relation":"FRIEND","recordStatus":"STORED","viewStatus":"PENDING",
            |"verificationStatus":"PENDING","requestedAt":"2026-06-21T03:07:26","approvedAt":null}]}}
            """.trimMargin().replace("\n", "")

        val box =
            json
                .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                .requireData()
                .toDomain(reporter)
                .single()

        assertEquals(DeliveryVerificationStatus.PENDING, box.verificationStatus)
        assertNull(box.approvedAt)
    }

    @Test
    fun `열람 신청 전 칸은 상태·시각이 통째로 없어도 디코드된다`() {
        val payload =
            """{"status":200,"code":200,"data":{"recordBoxes":[
            |{"receiverId":9,"accessCode":"c0ffee00-0000-4000-8000-000000000002","senderName":"이영희",
            |"receiverName":"김지은","relation":"MOTHER","recordStatus":"STORED","viewStatus":"REQUESTABLE"}]}}
            """.trimMargin().replace("\n", "")

        val box =
            json
                .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                .requireData()
                .toDomain(reporter)
                .single()

        assertEquals(DeliveryVerificationStatus.UNKNOWN, box.verificationStatus)
        assertNull(box.requestedAt)
        assertNull(box.approvedAt)
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun `같은 이메일의 다른 발신자 칸이 함께 와도 접근 코드로 갈린다`() {
        val payload =
            """{"status":200,"code":200,"data":{"recordBoxes":[
            |{"receiverId":4,"accessCode":"aaaaaaaa-0000-4000-8000-000000000001","senderName":"김혜성",
            |"receiverName":"김지은","recordStatus":"STORED","viewStatus":"VIEWABLE",
            |"verificationStatus":"APPROVED","approvedAt":"2026-07-29T16:00:10"},
            |{"receiverId":5,"accessCode":"bbbbbbbb-0000-4000-8000-000000000002","senderName":"박경민",
            |"receiverName":"김지은","recordStatus":"STORED","viewStatus":"VIEWABLE",
            |"verificationStatus":"APPROVED","approvedAt":"2026-08-01T09:00:00"}]}}
            """.trimMargin().replace("\n", "")

        val boxes =
            json
                .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                .requireData()
                .toDomain(reporter)

        assertEquals(2, boxes.size)
        val mine = boxes.first { it.masterKey == "bbbbbbbb-0000-4000-8000-000000000002" }
        assertEquals("박경민", mine.senderName)
        assertEquals("2026-08-01T09:00:00", mine.approvedAt)
    }

    @Test
    fun `기록함이 하나도 없으면 빈 목록이다`() {
        val payload = """{"status":200,"code":200,"data":{"recordBoxes":[]}}"""

        val boxes = json.decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload).requireData().toDomain(reporter)

        assertTrue(boxes.isEmpty())
    }

    /**
     * 실서버 응답 형태 그대로 (dev `afternote.kro.kr`, 2026-08-30 12:1x KST 실측).
     *
     * 시각이 **오프셋 없는 마이크로초 6자리**(`2026-08-25T18:44:02.585799`)로 온다 — 스키마의
     * `format: date-time` 은 RFC 3339 라 오프셋이 필수인데 실제 값은 그렇지 않다(BE#269).
     * 표시는 `T` 앞만 쓰므로 무사하지만, 누가 날짜 파서를 규격대로 바꾸면 여기서 깨진다.
     *
     * 접근 코드는 실측값이 아니라 자리만 맞춘 가짜다 — 실제 마스터 키는 열람 자격이라 넣지 않는다.
     */
    @Test
    fun `실서버 응답 형태 — 마이크로초가 붙은 시각도 그대로 도달한다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"recordBoxes":[
            |{"receiverId":14,"accessCode":"00000000-0000-4000-8000-00000000abcd","senderName":"김혜성",
            |"receiverName":"김지은","relation":"DAUGHTER","recordStatus":"STORED","viewStatus":"VIEWABLE",
            |"verificationStatus":"APPROVED","requestedAt":"2026-08-25T18:43:47.696636",
            |"approvedAt":"2026-08-25T18:44:02.585799"}]}}
            """.trimMargin().replace("\n", "")

        val box =
            json
                .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                .requireData()
                .toDomain(reporter)
                .single()

        assertEquals(DeliveryVerificationStatus.APPROVED, box.verificationStatus)
        assertEquals("2026-08-25T18:43:47.696636", box.requestedAt)
        assertEquals("2026-08-25T18:44:02.585799", box.approvedAt)
    }

    /**
     * 서버가 항상 채우는 필드는 누락이 **실패로 드러나야** 한다.
     *
     * `receiverName`(`Receiver.name` 은 DB NOT NULL)·`recordStatus`·`viewStatus`(둘 다 서버가 분기마다
     * 값을 반환)를 nullable 로 두면, 계약이 바뀌어도 「이름 없는 기록함」 이 화면까지 흘러간다.
     * #607 이 이 필드들을 도메인으로 올릴 때 폴백을 떠안지 않도록 여기서 막는다.
     */
    @Test
    fun `항상 오는 필드가 빠지면 디코드가 실패한다`() {
        listOf("receiverName", "recordStatus", "viewStatus").forEach { missing ->
            val payload = approvedBoxPayload(omit = missing)

            val failure =
                runCatching {
                    json
                        .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                        .requireData()
                        .toDomain(reporter)
                }.exceptionOrNull()

            assertNotNull("$missing 누락이 조용히 통과했다", failure)
        }
    }

    /**
     * 명시적 `null` 도 마찬가지다 — `coerceInputValues` 는 기본값이 있어야 동작하므로,
     * 기본값을 두지 않은 이 필드들을 구해 주지 않는다.
     */
    @Test
    fun `항상 오는 필드가 null 로 오면 디코드가 실패한다`() {
        listOf("receiverName", "recordStatus", "viewStatus").forEach { nulled ->
            val payload = approvedBoxPayload(nullify = nulled)

            val failure =
                runCatching {
                    json
                        .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                        .requireData()
                        .toDomain(reporter)
                }.exceptionOrNull()

            assertNotNull("$nulled 의 null 이 조용히 통과했다", failure)
        }
    }

    /** 승인된 칸 한 건짜리 응답. [omit] 은 키를 통째로 빼고, [nullify] 는 값을 `null` 로 바꾼다. */
    private fun approvedBoxPayload(
        omit: String? = null,
        nullify: String? = null,
    ): String {
        val fields =
            listOf(
                "receiverId" to "4",
                "accessCode" to "\"59c04a15-1f4a-4b2e-9a0c-2f4e8d7b6c31\"",
                "senderName" to "\"김혜성\"",
                "receiverName" to "\"김지은\"",
                "recordStatus" to "\"STORED\"",
                "viewStatus" to "\"VIEWABLE\"",
                "verificationStatus" to "\"APPROVED\"",
                "approvedAt" to "\"2026-07-29T16:00:10\"",
            ).filterNot { (key, _) -> key == omit }
                .joinToString(",") { (key, value) ->
                    "\"$key\":${if (key == nullify) "null" else value}"
                }

        return """{"status":200,"code":200,"data":{"recordBoxes":[{$fields}]}}"""
    }
}
