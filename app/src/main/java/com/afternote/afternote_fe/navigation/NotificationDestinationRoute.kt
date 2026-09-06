package com.afternote.afternote_fe.navigation

import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.ui.Route

/**
 * 알림 목적지 계약값을 앱 최상위 [Route]로 옮긴다 (#1111).
 *
 * `when`이 exhaustive라 [NotificationDestination]에 값을 더하면 여기가 컴파일로 막는다 —
 * 「보내는 쪽에만 추가하고 도착지를 안 만든 목적지」가 생기지 않는다.
 *
 * 이 매핑만 따로 파일을 갖는 이유는 **계약이 이동 배선과 다른 것**이기 때문이다.
 * «언제 옮기는가»(로그인 여부·큐 소비)와 실제 이동은 앱 루트 결선의 몫이고 그 결선은
 * Navigation 3 루트 전환 뒤 #1795 가 붙인다 — 이 함수는 «어디로 옮기는가»만 정한다.
 * 이쪽은 화면 없이 값만으로 전량 검증할 수 있어 `NotificationDestinationRouteTest`가
 * 목적지 4종을 통째로 훑는다.
 */
internal fun NotificationDestination.toRoute(): Route =
    when (this) {
        NotificationDestination.HOME -> Route.Home
        NotificationDestination.MIND_RECORD -> Route.MindRecord
        NotificationDestination.TIME_LETTER -> Route.TimeLetter
        NotificationDestination.AFTERNOTE -> Route.Afternote
    }
