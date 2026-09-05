package com.afternote.core.model.delivery

/**
 * 전달 대상 콘텐츠 유형. 세 도메인(타임레터·애프터노트·마인드레코드)의 콘텐츠를 아우른다.
 *
 * 이 패키지(수신자별 전달조건 도메인, 이슈 #427)는 서버가 전달조건을 유저단위 → (수신자 × 콘텐츠) 단위로
 * 재설계하며 신설된 축이다. 기존 유저단위 `DeliveryConditionType`
 * (NONE/INACTIVITY/SPECIFIC_DATE) 은 값·의미가 달라 재사용 불가라 별도 패키지에 두었고, 그 구 타입은 이슈 #428 에서 제거됐다.
 */
enum class DeliveryContentType {
    TIME_LETTER,
    AFTERNOTE,
    DAILY_QUESTION,
    DIARY,
    DEEP_THOUGHT,
}

/** 전달 조건 충족 방식. */
enum class DeliveryConditionType {
    /** 일정 기간 미활동 → 본인확인 유예 → 자동 전달. */
    INACTIVITY,

    /** 수신자가 서류를 제출하고 운영자가 승인 → 전달. */
    RECEIVER_REQUEST,
}

/** 미사용 자동 전달(INACTIVITY)의 기준 기간. RECEIVER_REQUEST 조건에서는 사용하지 않는다(null). */
enum class InactivityPeriod {
    THREE_MONTHS,
    SIX_MONTHS,
    ONE_YEAR,
}

/** 단일 조건의 진행 상태. */
enum class ConditionState {
    /** 조건이 걸려 있으나 아직 트리거 전. */
    ACTIVE,

    /**
     * INACTIVITY 안전장치 — 미사용이 감지됐지만 곧장 전달하지 않고 "정말 부재/사망이 맞는지"
     * 확인하는 유예 대기 상태. 이 기간(문서상 7일, [DeliveryConditionItem.gracePeriodStartedAt] 부터 카운트)
     * 에 사용자가 앱을 열면(로그인·토큰 재발급을 서버가 활동으로 집계) 본인 생존이 확인돼 조건이 취소·리셋되고, 무반응이면 부재 확정 →
     * FULFILLED. 잠깐 앱을 안 썼을 뿐인 산 사람의 유산이 열리는 오발동을 막는 완충 단계.
     */
    PENDING_CONFIRMATION,

    /** 수신자 요청 서류의 운영자 승인 대기. */
    WAITING_VERIFICATION,

    /** 조건 충족 완료 → 열람 가능. */
    FULFILLED,
}

/** 수신자 1인의 콘텐츠별 전달조건 묶음. */
data class ReceiverDeliveryConditions(
    val receiverId: Long,
    val conditions: List<DeliveryConditionItem>,
)

/**
 * 단일 (콘텐츠 × 조건) 항목.
 *
 * @property inactivityPeriod [DeliveryConditionType.INACTIVITY] 일 때만 채워진다.
 * @property state 조회 시점의 진행 상태. 설정(PUT) 요청에는 포함하지 않는다(서버 판정 값).
 * @property gracePeriodStartedAt 본인확인 유예 **시작** 시각(ISO-8601), 없으면 null. 전달 확정 시점은
 *   이 시각 + 유예 기간이다. 주의: 유예 기간(재설계 문서상 7일)은 응답에 없다 — "유예 D-N" 같은 남은
 *   시간을 표시하려면 그 상수가 필요한데 서버가 안 주므로 FE 하드코딩에 의존하게 된다(인증번호 TTL #421 과
 *   동일 패턴). UI 붙일 때 7일 확정 + 응답 포함 여부를 서버에 확인할 것.
 * @property fulfilledAt 조건 충족 확정 시각(ISO-8601), 없으면 null.
 */
data class DeliveryConditionItem(
    val contentType: DeliveryContentType,
    val conditionType: DeliveryConditionType,
    val inactivityPeriod: InactivityPeriod?,
    val state: ConditionState,
    val fulfilled: Boolean,
    val gracePeriodStartedAt: String?,
    val fulfilledAt: String?,
)
