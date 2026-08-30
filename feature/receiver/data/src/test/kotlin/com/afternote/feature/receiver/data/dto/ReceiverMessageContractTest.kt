package com.afternote.feature.receiver.data.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `GET /receiver-auth/message` 실서버 응답 계약 회귀 가드 (#209).
 *
 * 페이로드는 2026-06-10 라이브 서버(`afternote.kro.kr`) 실응답 캡처 원문 — `createdAt` 은
 * BE `LocalDateTime` 직렬화라 타임존 표기 없는 마이크로초 포함 형태로 온다 (BE 커밋 5af499c8).
 * 프로덕션 경로(`ReceiverAuthRepositoryImpl.getSenderMessage`)와 동일하게
 * Json 디코드 → `requireData()` → `toDomain()` 을 통과시킨다 — Json 설정은
 * `NetworkModule.provideJson` 과 동일 (ignoreUnknownKeys).
 */
class ReceiverMessageContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `라이브 서버 실응답 캡처 - 디코드부터 Hero 카드 date 값까지 도달`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"senderName":"큐에이발신자","message":"실서버 통신 검증용 한 마디입니다. 잘 지내렴.","createdAt":"2026-06-10T15:27:14.141643"}}"""

        val info = json.decodeFromString<BaseResponse<ReceiverMessageDto>>(payload).requireData().toDomain()

        assertEquals("큐에이발신자", info.senderName)
        assertEquals("실서버 통신 검증용 한 마디입니다. 잘 지내렴.", info.message)
        // ReceiverHomeViewModel 이 이 값을 그대로 SenderMessage.date 로 사용한다.
        assertEquals("2026.06.10", info.createdAt)
    }

    @Test
    fun `createdAt·message 없는 구버전 응답 - 디코드 깨지지 않고 null 유지`() {
        val payload = """{"status":200,"code":200,"data":{"senderName":"김철수"}}"""

        val info = json.decodeFromString<BaseResponse<ReceiverMessageDto>>(payload).requireData().toDomain()

        assertNull(info.message)
        assertNull(info.createdAt)
    }
}
