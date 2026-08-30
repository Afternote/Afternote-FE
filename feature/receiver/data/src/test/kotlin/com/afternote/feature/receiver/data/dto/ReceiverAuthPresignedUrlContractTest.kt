package com.afternote.feature.receiver.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `POST /receiver-auth/presigned-url` 계약 회귀 가드 (#915).
 *
 * 2026-08-28 라이브 OpenAPI 기준 요청의 `extension`·`contentLength`는 모두 required다.
 * 응답의 `contentLength`는 S3 PUT의 `Content-Length`와 일치시켜야 한다.
 */
class ReceiverAuthPresignedUrlContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `요청에 확장자와 실제 파일 크기가 모두 실린다`() {
        val encoded =
            json.encodeToString(
                ReceiverAuthPresignedUrlRequestDto.serializer(),
                ReceiverAuthPresignedUrlRequestDto(
                    extension = "pdf",
                    contentLength = 357L,
                ),
            )

        assertEquals("""{"extension":"pdf","contentLength":357}""", encoded)
    }

    @Test
    fun `응답의 PUT contentLength를 읽는다`() {
        val decoded =
            json.decodeFromString(
                ReceiverAuthPresignedUrlDto.serializer(),
                """
                {
                  "presignedUrl": "https://s3.example/put?sig=abc",
                  "fileKey": "receiver-auth/staging/document.pdf",
                  "fileUrl": "https://cdn.example/receiver-auth/staging/document.pdf",
                  "contentType": "application/pdf",
                  "contentLength": 357,
                  "maxContentLength": 10485760
                }
                """,
            )

        assertEquals(357L, decoded.contentLength)
    }
}
