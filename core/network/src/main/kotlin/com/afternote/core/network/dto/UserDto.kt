package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DeliveryConditionTypeDto {
    @SerialName("NONE")
    NONE,

    @SerialName("INACTIVITY")
    INACTIVITY,

    @SerialName("SPECIFIC_DATE")
    SPECIFIC_DATE,
}

// ========================================
// Request
// ========================================

@Serializable
data class UserCreateReceiverRequestDto(
    @SerialName("name") val name: String,
    @SerialName("relation") val relation: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("email") val email: String? = null,
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
data class SocialAccountLinkRequestDto(
    @SerialName("accessToken") val accessToken: String,
)

@Serializable
data class DeliveryConditionRequestDto(
    @SerialName("conditionType") val conditionType: DeliveryConditionTypeDto,
    @SerialName("inactivityPeriodDays") val inactivityPeriodDays: Int? = null,
    @SerialName("specificDate") val specificDate: String? = null,
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

@Serializable
data class ReceiverListDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("name") val name: String,
    @SerialName("relation") val relation: String,
    @SerialName("authCode") val authCode: String,
)

@Serializable
data class ReceiverDetailDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("name") val name: String,
    @SerialName("relation") val relation: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("dailyQuestionCount") val dailyQuestionCount: Int,
    @SerialName("timeLetterCount") val timeLetterCount: Int,
    @SerialName("afterNoteCount") val afterNoteCount: Int,
    @SerialName("message") val message: String? = null,
    @SerialName("authCode") val authCode: String,
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

@Serializable
data class DeliveryConditionDto(
    @SerialName("conditionType") val conditionType: DeliveryConditionTypeDto,
    @SerialName("inactivityPeriodDays") val inactivityPeriodDays: Int? = null,
    @SerialName("specificDate") val specificDate: String? = null,
    @SerialName("conditionFulfilled") val conditionFulfilled: Boolean,
    @SerialName("conditionMet") val conditionMet: Boolean,
)

// --- GET/POST/DELETE /users/connected-accounts (소셜 계정 연동) ---

/**
 * GET 응답은 provider 별 boolean + email, POST/DELETE 응답은 boolean 만 — email 은 null 로 내려올 수 있어 모두 optional.
 */
@Serializable
data class ConnectedAccountsDto(
    val local: Boolean,
    val google: Boolean,
    val naver: Boolean,
    val kakao: Boolean,
    val apple: Boolean,
    val localEmail: String? = null,
    val googleEmail: String? = null,
    val naverEmail: String? = null,
    val kakaoEmail: String? = null,
    val appleEmail: String? = null,
)

/** POST /users/connected-accounts/{provider} 요청 body. 소셜 SDK 로 받은 access token 을 전달. */
@Serializable
data class ConnectAccountRequestDto(
    val accessToken: String,
)
