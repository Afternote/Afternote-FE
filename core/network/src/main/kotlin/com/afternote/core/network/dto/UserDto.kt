package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
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

// --- GET /users/receivers/{receiverId}/daily-questions (time-letters, after-notes are in Received API) ---

// --- GET /users/receivers/{receiverId}/mind-records (일기, 깊은 생각, 데일리 질문 답변 통합 조회) ---

/**
 * 수신인별 마음의 기록 항목 DTO.
 * type: DIARY, DEEP_THOUGHT, DAILY_QUESTION
 * DAILY_QUESTION: question, answer, createdAt
 * DIARY/DEEP_THOUGHT: title, content, date
 */
@Serializable
data class ReceiverMindRecordItemDto(
    val recordId: Long,
    val type: String,
    val titleOrQuestion: String? = null,
    val contentOrAnswer: String? = null,
    val recordDate: String,
)

@Serializable
data class ReceiverMindRecordsResponseDto(
    val items: List<ReceiverMindRecordItemDto> = emptyList(),
    val hasNext: Boolean = false,
)

// --- daily-questions (기존) ---

@Serializable
data class DailyQuestionAnswerItemDto(
    val dailyQuestionAnswerId: Long,
    val question: String,
    val answer: String,
    @SerialName("createdAt") val recordDate: String,
)

@Serializable
data class ReceiverDailyQuestionsResponseDto(
    val items: List<DailyQuestionAnswerItemDto> = emptyList(),
    val hasNext: Boolean = false,
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
