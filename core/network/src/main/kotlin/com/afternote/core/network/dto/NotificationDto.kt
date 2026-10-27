package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ========================================
// Request
// ========================================

/**
 * `PUT users/me/notification-preferences` 요청 (#2321).
 *
 * [time] 은 한국 시간 `HH:mm` 이다. 서버는 초·소수초가 0 이 아니면 거부한다.
 * [weekdays] 는 `MONDAY`…`SUNDAY`, [weeklyGoal] 은 1~7 이다.
 */
@Serializable
data class NotificationPreferencesRequestDto(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("time") val time: String,
    @SerialName("weekdays") val weekdays: List<String>,
    @SerialName("weeklyGoal") val weeklyGoal: Int,
)

/** `PUT users/me/notification-preferences/delegate` 요청. 이름 100자·연락처 20자 이하, 둘 다 필수. */
@Serializable
data class NotificationDelegateRequestDto(
    @SerialName("name") val name: String,
    @SerialName("phone") val phone: String,
)

/** `POST users/me/notification-preferences/anniversaries` 요청. 존재하는 달력 날짜만 받는다. */
@Serializable
data class NotificationAnniversaryRequestDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("month") val month: Int,
    @SerialName("day") val day: Int,
)

// ========================================
// Response
// ========================================

/**
 * `GET·PUT users/me/notification-preferences` 응답.
 *
 * [time] 은 서버 `LocalTime` 을 Jackson 이 쓴 그대로다. 기본 설정이면 `"18:00:00"` 문자열이지만,
 * 서버 직렬화 설정에 따라 `[18,0]` 배열로도 온다. 해석은 data 계층이 한다 — 여기서 한 형태로 고정하면
 * 서버 설정 하나에 화면 전체가 파싱 오류로 죽는다.
 */
@Serializable
data class NotificationPreferencesDto(
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("time") val time: JsonElement,
    @SerialName("weekdays") val weekdays: List<String>,
    @SerialName("weeklyGoal") val weeklyGoal: Int,
)

/** 대리인. 지정 전이면 둘 다 null 이다. */
@Serializable
data class NotificationDelegateDto(
    @SerialName("name") val name: String? = null,
    @SerialName("phone") val phone: String? = null,
)

@Serializable
data class NotificationAnniversaryDto(
    @SerialName("id") val id: Long,
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("month") val month: Int,
    @SerialName("day") val day: Int,
)

/**
 * `POST users/me/notification-preferences/entry` 응답.
 *
 * [easyQuestions] 는 작성 화면에 보여 줄 문구다. daily question ID 가 아니다.
 */
@Serializable
data class NotificationEntryDto(
    @SerialName("showEasyQuestions") val showEasyQuestions: Boolean,
    @SerialName("easyQuestions") val easyQuestions: List<String>,
    @SerialName("procedureCancelled") val procedureCancelled: Boolean,
)

/** `GET users/notifications` 응답. [unreadCount] 는 이 페이지가 아니라 전체 미읽음 수다. */
@Serializable
data class NotificationInboxDto(
    @SerialName("unreadCount") val unreadCount: Long,
    @SerialName("items") val items: List<NotificationInboxItemDto>,
)

/**
 * 알림함 한 줄. [data] 는 FCM data 와 같은 문자열 맵이다 — 탭하면 같은 목적지 해석을 탄다.
 * [status] 는 서버 처리 상태라 화면에 보여 주지 않는다.
 */
@Serializable
data class NotificationInboxItemDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("data") val data: Map<String, String>,
    @SerialName("status") val status: String,
    @SerialName("readAt") val readAt: String? = null,
)
