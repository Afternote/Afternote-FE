package com.afternote.core.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `POST /files/presigned-url` 요청 계약 가드 (#950).
 *
 * 명세는 세 필드를 모두 required 로 선언한다.
 * ```
 * PresignedUrlRequest  required: [contentLength, directory, extension]
 * ```
 * `contentLength` 가 빠지면 서버가 `400 파일 크기는 필수입니다` 로 거절해 **앱의 모든 이미지
 * 업로드가 실패한다** (2026-08-23 실측). 세 필드가 실제로 나가는지 여기서 고정한다.
 */
class PresignedUrlContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `요청에 세 필드가 모두 실린다`() {
        val encoded =
            json.encodeToString(
                PresignedUrlRequestDto.serializer(),
                PresignedUrlRequestDto(directory = "mindrecords", extension = "png", contentLength = 357L),
            )

        assertTrue(encoded.contains("\"directory\":\"mindrecords\""))
        assertTrue(encoded.contains("\"extension\":\"png\""))
        assertTrue("파일 크기가 빠지면 서버가 400 으로 거절한다", encoded.contains("\"contentLength\":357"))
    }

    @Test
    fun `크기가 0 이어도 필드는 생략되지 않는다`() {
        // 기본값을 두면 kotlinx 가 기본값과 같은 값을 생략할 수 있다 — 크기는 언제나 명시한다.
        val encoded =
            json.encodeToString(
                PresignedUrlRequestDto.serializer(),
                PresignedUrlRequestDto(directory = "profiles", extension = "jpg", contentLength = 0L),
            )

        assertTrue(encoded.contains("\"contentLength\":0"))
    }
}
