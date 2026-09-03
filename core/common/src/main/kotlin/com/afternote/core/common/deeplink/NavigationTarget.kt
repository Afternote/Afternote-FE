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

    /** 받은 기록함 — 수신자가 등록한 발신자 카드 목록. */
    data object ReceivedRecordBox : NavigationTarget {
        override val canonicalPath: String = "/received"
        override val requiredGates: List<AuthGate> = listOf(AuthGate.LOGIN)
    }

    /**
     * 발신자 상세.
     *
     * [senderId] 는 지금 **프로세스 메모리에만 사는 클라이언트 로컬 UUID** 다(`SenderRegistry`, #215 —
     * 발신자 라벨 API 가 아직 없다). 그래서 이 경로는 형식이 맞아도 앱 밖에서 온 링크로는 사실상
     * 해석되지 않는다. 파서는 **형식만** 판정하고, 실제 조회 실패는 소비처가 fail-closed 로 처리한다.
     * 서버 발신자 식별자가 생기면 이 행의 ID 형식을 그때 다시 확정한다.
     */
    data class ReceivedSenderDetail(
        val senderId: String,
    ) : NavigationTarget {
        override val canonicalPath: String = "/received/senders/$senderId"
        override val requiredGates: List<AuthGate> = listOf(AuthGate.LOGIN)
    }

    /** 수신 애프터노트 상세 — 발신자별 본인인증을 통과해야 열린다. */
    data class ReceivedAfternoteDetail(
        val afternoteId: Long,
    ) : NavigationTarget {
        override val canonicalPath: String = "/received/afternote/$afternoteId"
        override val requiredGates: List<AuthGate> =
            listOf(AuthGate.LOGIN, AuthGate.RECEIVER_IDENTITY)
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
