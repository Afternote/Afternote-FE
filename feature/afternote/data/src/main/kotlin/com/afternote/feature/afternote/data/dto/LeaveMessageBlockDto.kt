package com.afternote.feature.afternote.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 서버 `leaveMessage` 배열의 원소.
 *
 * 응답 쪽은 저장된 JSON 이 `body` 를 빠뜨린 형태여도 파싱이 통째로 실패하지 않도록 nullable 로 받고,
 * 도메인 경계에서 빈 문자열로 좁힌다. 요청 쪽은 도메인이 이미 non-null [body] 만 들고 있다.
 */
@Serializable
data class LeaveMessageBlockDto(
    @SerialName("title") val title: String? = null,
    @SerialName("body") val body: String? = null,
)
