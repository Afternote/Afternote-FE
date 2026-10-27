package com.afternote.feature.setting.domain

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * 서버 알림(BE#317)의 사용자 설정 계약 (#2321) — 기록 알림 시각·요일·주간 목표, 대리인, 수신인 기념일.
 *
 * 기존 [SettingNotificationRepository] 의 카테고리 토글(타임레터·마음의 기록·애프터노트)과는 서버에서
 * 따로 적용된다. 마음의 기록 토글이 꺼져 있으면 여기 설정과 무관하게 기록 알림이 오지 않는다.
 */
public interface NotificationPreferenceRepository {
    public suspend fun getPreferences(): Result<NotificationPreferences>

    /** 바꾸면 서버가 대기 중이던 이전 기록 알림을 취소한다. */
    public suspend fun updatePreferences(preferences: NotificationPreferences): Result<NotificationPreferences>

    /** 지정한 대리인. 아직 없으면 null. */
    public suspend fun getDelegate(): Result<NotificationDelegate?>

    /** 바꾸면 이전 대리인에게 간 확인 링크가 무효가 된다. */
    public suspend fun updateDelegate(delegate: NotificationDelegate): Result<Unit>

    public suspend fun deleteDelegate(): Result<Unit>

    public suspend fun getAnniversaries(): Result<List<NotificationAnniversary>>

    public suspend fun addAnniversary(
        receiverId: Long,
        month: Int,
        day: Int,
    ): Result<NotificationAnniversary>

    public suspend fun deleteAnniversary(anniversaryId: Long): Result<Unit>
}

/**
 * 기록 알림(B) 설정.
 *
 * @property time 한국 시간. 서버는 분 단위까지만 받는다.
 * @property weekdays 알림을 받을 요일. 비어 있으면 서버가 거부한다.
 * @property weeklyGoal 주간 목표 기록 수(1~7). 알림 요일과는 별개다.
 */
public data class NotificationPreferences(
    val enabled: Boolean,
    val time: LocalTime,
    val weekdays: Set<DayOfWeek>,
    val weeklyGoal: Int,
)

/** 오래 접속하지 않을 때 연락받을 사람. 수신인과 별개다. */
public data class NotificationDelegate(
    val name: String,
    val phone: String,
)

/** 수신인 생일. 2월 29일은 평년에 2월 28일로 안내된다. */
public data class NotificationAnniversary(
    val id: Long,
    val receiverId: Long,
    val month: Int,
    val day: Int,
)
