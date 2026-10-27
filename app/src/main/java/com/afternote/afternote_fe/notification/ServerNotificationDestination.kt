package com.afternote.afternote_fe.notification

import com.afternote.core.common.notification.NotificationDestination

/**
 * 서버 알림 계약의 `destination` 값(BE `PushPayload`)을 앱이 지금 열 수 있는 목적지로 옮긴다 (#2321).
 *
 * 서버 목적지는 상세 화면까지 가리키지만 앱의 알림 진입은 아직 최상위 탭까지만 연다. 그래서 상세는
 * 그 상세를 품은 탭으로 접고, 앱에 화면이 없거나 탭이 아닌 목적지(대리인·보관 확인·주간 리포트·쉬운
 * 질문·수신자·초대·웹 링크·결제)는 [NotificationDestination.HOME] 으로 보낸다. 계약 밖 값·null 도 HOME 이다.
 * 상세 화면 진입점이 생기면 여기서 갈래를 늘린다.
 */
internal object ServerNotificationDestination {
    fun toAppDestination(value: String?): NotificationDestination =
        when (value?.trim()) {
            "RECORD_LIST", "RECORD_DETAIL", "DAILY_QUESTION" -> NotificationDestination.MIND_RECORD
            "TIME_LETTER_LIST", "TIME_LETTER_DETAIL", "TIME_LETTER_COMPOSE" -> NotificationDestination.TIME_LETTER
            else -> NotificationDestination.HOME
        }
}
