package com.afternote.core.network.dto

import kotlinx.serialization.Serializable

// TODO: 리팩토링
@Serializable
data class UserResponse(
    val name: String,
    val email: String,
    val phone: String? = null,
    val profileImageUrl: String? = null,
)

@Serializable
data class UserUpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    val profileImageUrl: String? = null,
)

/** GET /users/push-settings 응답 data. 푸시 알림 수신 설정 (timeLetter, mindRecord, afterNote). */
@Serializable
data class UserPushSettingResponse(
    val timeLetter: Boolean,
    val mindRecord: Boolean,
    val afterNote: Boolean,
)

@Serializable
data class UserUpdatePushSettingRequest(
    val timeLetter: Boolean? = null,
    val mindRecord: Boolean? = null,
    val afterNote: Boolean? = null,
)

// --- Receivers (GET /users/receivers, POST /users/receivers, GET /users/receivers/{receiverId}) ---

@Serializable
data class ReceiverItemDto(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val mindRecordDeliveryEnabled: Boolean = true,
)

@Serializable
data class RegisterReceiverRequestDto(
    val name: String,
    val phone: String? = null,
    val relation: String,
    val email: String? = null,
)

@Serializable
data class RegisterReceiverResponseDto(
    val receiverId: Long,
)

@Serializable
data class ReceiverDetailResponseDto(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val phone: String? = null,
    val email: String? = null,
    val dailyQuestionCount: Int = 0,
    val timeLetterCount: Int = 0,
    val afterNoteCount: Int = 0,
)

// --- GET /users/delivery-condition, PATCH /users/delivery-condition (전달 조건) ---

/**
 * 전달 조건 타입 - 콘텐츠가 수신자에게 전달되는 조건.
 */
@Serializable
enum class DeliveryConditionTypeDto {
    NONE,
    DEATH_CERTIFICATE,
    INACTIVITY,
    SPECIFIC_DATE,
}

/**
 * GET /users/delivery-condition 응답 data. 전달 조건 설정 응답.
 */
@Serializable
data class DeliveryConditionResponseDto(
    val conditionType: DeliveryConditionTypeDto,
    val inactivityPeriodDays: Int? = null,
    val specificDate: String? = null,
    val leaveMessage: String? = null,
    val conditionFulfilled: Boolean,
    val conditionMet: Boolean,
)

/**
 * PATCH /users/delivery-condition 요청 body.
 */
@Serializable
data class DeliveryConditionRequestDto(
    val conditionType: DeliveryConditionTypeDto,
    val inactivityPeriodDays: Int? = null,
    val specificDate: String? = null,
    val leaveMessage: String? = null,
)

// --- GET/POST/DELETE /users/connected-accounts (소셜 계정 연동) ---

/**
 * GET 응답은 provider 별 boolean + email, POST/DELETE 응답은 boolean 만 — email 은 null 로 내려올 수 있어 모두 optional.
 */
@Serializable
data class ConnectedAccountsResponseDto(
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
