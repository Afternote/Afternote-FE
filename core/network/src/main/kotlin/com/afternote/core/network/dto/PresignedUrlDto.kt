package com.afternote.core.network.dto

import kotlinx.serialization.Serializable

/**
 * `POST /files/presigned-url` 요청.
 *
 * 세 필드 모두 명세가 required 로 선언한 값이다. [contentLength] 가 빠지면 서버가
 * `400 파일 크기는 필수입니다` 로 거절해 **모든 이미지 업로드가 실패한다** (#950).
 * 기본값을 두지 않는 이유도 같다 — 크기를 모른 채 요청이 나가면 안 된다.
 */
@Serializable
data class PresignedUrlRequestDto(
    val directory: String,
    val extension: String,
    /** 업로드할 파일 크기(바이트). Presigned PUT 의 `Content-Length` 와 같아야 한다. */
    val contentLength: Long,
)

@Serializable
data class PresignedUrlDto(
    val presignedUrl: String,
    val fileKey: String,
    val fileUrl: String,
    val contentType: String,
)
