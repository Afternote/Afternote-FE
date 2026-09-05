package com.afternote.feature.receiver.data.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `POST /receiver-auth/email/auth-code`·`POST /receiver-auth/email/verify` 응답 계약 회귀 가드 (#407, #454).
 *
 * 페이로드는 2026-06-11 라이브 Swagger(`afternote.kro.kr/v3/api-docs`) 스키마 기반 합성 —
 * verify 성공 응답은 6자리 인증번호를 메일로 받아야 해서 자동 캡처 불가
 * (필드 구성·타입은 Swagger 와 BE 소스로 확정, 이슈 #407 본문).
 * 프로덕션 경로(`ReceiverAuthRepositoryImpl`)와 동일하게 Json 디코드 → `requireData()`/`requireStatus()`
 * → `toDomain()` 을 통과시킨다 — Json 설정은 `NetworkModule.provideJson` 과 동일
 * (ignoreUnknownKeys).
 *
 * verify 성공 응답을 **masterKey 동봉·제거 두 형태 모두** 가드하는 이유: 서버가 마스터 키와 동일한
 * `masterKey` 를 응답에서 제거할 예정이라([ReceiverEmailAuthVerifyDto] 참고), 제거 배포가 언제 나가든
 * 파싱이 깨지지 않아야 한다 (#454).
 *
 * 에러 응답(404 code 1901 / 400 code 1902·1903)은 HTTP 4xx 라 Retrofit CallAdapter가 가로채는 경로 —
 * 그 이후의 도메인 예외 변환은 `ReceiverAuthRepositoryImplEmailAuthTest` 가 가드한다.
 */
class ReceiverEmailAuthContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `email-verify 성공 응답(masterKey 동봉) - 잔여 키를 무시하고 도메인 모델까지 도달`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"receiverId":3,"receiverName":"큐에이수신자","senderName":"큐에이발신자","accessCode":"123e4567-e89b-12d3-a456-426614174000"}}"""

        val result = json.decodeFromString<BaseResponse<ReceiverEmailAuthVerifyDto>>(payload).requireData().toDomain()

        assertEquals(3L, result.receiverId)
        assertEquals("큐에이수신자", result.receiverName)
        assertEquals("큐에이발신자", result.senderName)
    }

    @Test
    fun `email-verify 성공 응답(masterKey 제거 후) - 디코드 성공`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"receiverId":3,"receiverName":"큐에이수신자","senderName":"큐에이발신자"}}"""

        val result = json.decodeFromString<BaseResponse<ReceiverEmailAuthVerifyDto>>(payload).requireData().toDomain()

        assertEquals(3L, result.receiverId)
        assertEquals("큐에이수신자", result.receiverName)
        assertEquals("큐에이발신자", result.senderName)
    }

    @Test
    fun `auth-code 성공 응답(data 없음) - BaseResponse_Unit 디코드 + requireStatus 무사 통과`() {
        val payload = """{"status":200,"code":200,"message":"성공","data":null}"""

        json.decodeFromString<BaseResponse<Unit>>(payload).requireStatus()
    }
}
