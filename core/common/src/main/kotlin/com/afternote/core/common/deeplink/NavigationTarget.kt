package com.afternote.core.common.deeplink

/**
 * App Link·FCM `data.link` 가 가리키는 **엔진 중립** 목적지 (#924).
 *
 * Nav2 `Route` 도 Navigation 3 `NavKey` 도 아니다. 링크 계약은 서버·브라우저·알림이 공유하는 외부
 * 계약이라 앱 안의 navigation 엔진 교체(#1698·#1702)와 수명이 다르다 — 둘을 같은 타입으로 묶으면
 * 엔진을 갈아탈 때 외부 계약이 함께 흔들린다. 그래서 이 모듈은 화면 그래프를 알지 않고, 실제
 * 화면 매핑은 앱 루트의 navigator adapter 가 갖는다(`NotificationDestination` 과 같은 이유·같은 층).
 *
 * [canonicalPath] 는 이 목적지를 가리키는 **유일한** 정규 경로다. 파서는 정규형만 받는다 —
 * 끝 슬래시·대문자·퍼센트 인코딩 변형은 전부 거절이다([AfternoteAppLinkParser]).
 *
 * @property canonicalPath `/` 로 시작하고 끝 슬래시가 없는 정규 경로.
 * @property requiredGates 진입 전 통과해야 하는 관문. [AuthGate] 순서대로 오름차순이며 비어 있지 않다.
 */
sealed interface NavigationTarget {
    val canonicalPath: String
    val requiredGates: List<AuthGate>

    /** 링크가 사이트 루트를 가리킬 때의 목적지이자, 거절된 링크의 안전한 기본 진입([AppLinkResolution.Rejected.fallback]). */
    data object Home : NavigationTarget {
        override val canonicalPath: String = "/"
        override val requiredGates: List<AuthGate> = listOf(AuthGate.LOGIN)
    }

    /** 작성자 애프터노트 홈. */
    data object AfternoteHome : NavigationTarget {
        override val canonicalPath: String = "/afternote"
        override val requiredGates: List<AuthGate> = listOf(AuthGate.LOGIN, AuthGate.BIOMETRIC)
    }

    /** 작성자 애프터노트 상세. */
    data class AfternoteDetail(
        val afternoteId: Long,
    ) : NavigationTarget {
        override val canonicalPath: String = "/afternote/$afternoteId"
        override val requiredGates: List<AuthGate> = listOf(AuthGate.LOGIN, AuthGate.BIOMETRIC)
    }

    /** 타임레터 상세. */
    data class TimeLetterDetail(
        val timeLetterId: Long,
    ) : NavigationTarget {
        override val canonicalPath: String = "/timeletter/$timeLetterId"
        override val requiredGates: List<AuthGate> = listOf(AuthGate.LOGIN)
    }

    /** 데일리질문 작성. */
    data object DailyQuestionCompose : NavigationTarget {
        override val canonicalPath: String = "/mindrecord/daily-question"
        override val requiredGates: List<AuthGate> = listOf(AuthGate.LOGIN)
    }

    /** 알림 설정. */
    data object NotificationSettings : NavigationTarget {
        override val canonicalPath: String = "/settings/notification"
        override val requiredGates: List<AuthGate> = listOf(AuthGate.LOGIN)
    }
}
