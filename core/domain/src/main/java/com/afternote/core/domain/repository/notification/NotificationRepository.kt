package com.afternote.core.domain.repository.notification

/**
 * 서버가 만든 알림(인앱·푸시 공통 ID)과 앱 진입 문맥을 다루는 계약 (#2321).
 *
 * 푸시 payload 는 발송 시점의 사진이다. 탭한 시점에는 대상이 지워졌거나 이미 답한 질문일 수 있어
 * 화면을 고르기 전에 [target] 으로 다시 묻는다. payload 자체는 열람 권한이 아니다.
 */
interface NotificationRepository {
    suspend fun target(notificationId: Long): Result<NotificationTarget>

    suspend fun markRead(notificationId: Long): Result<Unit>

    /** 알림함 첫 페이지(최대 50개)와 전체 미읽음 수. */
    suspend fun inbox(): Result<NotificationInbox>

    /**
     * 앱에 들어왔다고 알린다. 서버는 이걸로 활동 시각을 갱신하고, 오래 비웠다 돌아온 사용자에게 줄
     * 쉬운 질문과 진행 중이던 보관 확인 절차가 취소됐는지를 돌려준다.
     */
    suspend fun reportEntry(): Result<NotificationEntry>
}

/**
 * 알림을 탭했을 때 지금 열어야 할 곳.
 *
 * @property data FCM data 와 같은 문자열 맵(`destination` 과 그 목적지의 ID·`action`). 앱 화면으로의
 *   번역은 app 몫이다.
 * @property deleted 대상이 지워졌다. [data] 는 이미 그 목록을 가리킨다.
 * @property toast 서버가 정한 안내 문구. 없으면 null.
 */
data class NotificationTarget(
    val data: Map<String, String>,
    val deleted: Boolean,
    val toast: String?,
)

data class NotificationInbox(
    val unreadCount: Long,
    val items: List<NotificationInboxItem>,
)

/** 알림함 한 줄. [data] 는 FCM data 와 같은 형식이라 탭하면 같은 목적지 해석을 탄다. */
data class NotificationInboxItem(
    val id: Long,
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val isRead: Boolean,
)

/**
 * 앱 진입 문맥.
 *
 * @property easyQuestions 오래 비웠다 돌아온 사용자에게 보여 줄 쉬운 질문 문구. 보여 줄 때가 아니면 빈 목록이다.
 * @property procedureCancelled 이번 진입으로 진행 중이던 보관 확인 절차가 취소됐다.
 */
data class NotificationEntry(
    val easyQuestions: List<String>,
    val procedureCancelled: Boolean,
)
