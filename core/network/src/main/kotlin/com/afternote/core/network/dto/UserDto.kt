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
