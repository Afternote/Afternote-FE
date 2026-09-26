package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ========================================
// Request
// ========================================

@Serializable
data class UserCreateReceiverRequestDto(
    @SerialName("name") val name: String,
    @SerialName("relation") val relation: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("email") val email: String,
    @SerialName("message") val message: String? = null,
)

@Serializable
data class UserPatchReceiverRequestDto(
    @SerialName("name") val name: String,
    @SerialName("phone") val phone: String,
    @SerialName("relation") val relation: String,
    @SerialName("email") val email: String,
)

@Serializable
data class UserUpdateReceiverMessageRequestDto(
    @SerialName("message") val message: String,
)

@Serializable
data class UserUpdateProfileRequestDto(
    @SerialName("name") val name: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,
)

@Serializable
data class UserUpdatePushSettingRequestDto(
    @SerialName("timeLetter") val timeLetter: Boolean? = null,
    @SerialName("mindRecord") val mindRecord: Boolean? = null,
    @SerialName("afterNote") val afterNote: Boolean? = null,
)

@Serializable
data class UserUpdateMarketingConsentRequestDto(
    @SerialName("sms") val sms: Boolean? = null,
    @SerialName("email") val email: Boolean? = null,
    @SerialName("push") val push: Boolean? = null,
)

@Serializable
data class SocialAccountLinkRequestDto(
    @SerialName("accessToken") val accessToken: String,
)

// ========================================
// Response
// ========================================

@Serializable
data class UserDto(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,
)

/**
 * `GET /users/receivers` 목록 항목 — 서버가 내려주는 세 필드뿐이다 (#2105).
 * `relation` 은 DB 가 null 을 허용해 그대로 비어 온다.
 */
@Serializable
data class ReceiverListDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("name") val name: String,
    @SerialName("relation") val relation: String? = null,
)

/**
 * `GET /users/receivers/{receiverId}` 상세 — 서버 `ReceiverDetailResponse` 의 9개 필드뿐이다 (#2155).
 * `authCode` 는 BE#289 이후 오지 않고, `relation` 은 DB 가 null 을 허용해 미입력이면 null 로 온다.
 */
@Serializable
data class ReceiverDetailDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("name") val name: String,
    @SerialName("relation") val relation: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("dailyQuestionCount") val dailyQuestionCount: Int,
    @SerialName("timeLetterCount") val timeLetterCount: Int,
    @SerialName("afterNoteCount") val afterNoteCount: Int,
    @SerialName("message") val message: String? = null,
)

@Serializable
data class UserCreateReceiverDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("authCode") val authCode: String,
)

@Serializable
data class UserPatchReceiverDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("name") val name: String,
    @SerialName("phone") val phone: String,
    @SerialName("relation") val relation: String,
    @SerialName("email") val email: String,
)

@Serializable
data class UserPushSettingDto(
    @SerialName("timeLetter") val timeLetter: Boolean,
    @SerialName("mindRecord") val mindRecord: Boolean,
    @SerialName("afterNote") val afterNote: Boolean,
)

@Serializable
data class UserMarketingConsentDto(
    @SerialName("sms") val sms: Boolean,
    @SerialName("email") val email: Boolean,
    @SerialName("push") val push: Boolean,
)

@Serializable
data class UserConnectedAccountDto(
    @SerialName("local") val local: Boolean,
    @SerialName("google") val google: Boolean,
    @SerialName("naver") val naver: Boolean,
    @SerialName("kakao") val kakao: Boolean,
    @SerialName("apple") val apple: Boolean,
    @SerialName("localEmail") val localEmail: String? = null,
    @SerialName("googleEmail") val googleEmail: String? = null,
    @SerialName("naverEmail") val naverEmail: String? = null,
    @SerialName("kakaoEmail") val kakaoEmail: String? = null,
    @SerialName("appleEmail") val appleEmail: String? = null,
)
