package com.afternote.core.domain.repository.notification

/**
 * 서버가 만든 알림(인앱·푸시 공통 ID)의 현재 상태를 묻는 계약 (#2321).
 *
 * 푸시 payload 는 발송 시점의 사진이다. 탭한 시점에는 대상이 지워졌거나 이미 답한 질문일 수 있어
 * 화면을 고르기 전에 서버에 다시 묻는다. payload 자체는 열람 권한이 아니다.
 */
interface NotificationRepository {
    suspend fun target(notificationId: Long): Result<NotificationTarget>

    suspend fun markRead(notificationId: Long): Result<Unit>
}

/**
 * 알림을 탭했을 때 지금 열어야 할 곳.
 *
 * @property destination 서버 목적지 계약값(`HOME`, `TIME_LETTER_DETAIL` …). 앱 화면으로의 번역은 app 몫이다.
 * @property deleted 대상이 지워졌다. [destination] 은 이미 그 목록을 가리킨다.
 * @property toast 서버가 정한 안내 문구. 없으면 null.
 */
data class NotificationTarget(
    val destination: String?,
    val deleted: Boolean,
    val toast: String?,
)
