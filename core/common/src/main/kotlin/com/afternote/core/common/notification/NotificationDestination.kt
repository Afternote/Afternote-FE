package com.afternote.core.common.notification

/**
 * 알림 탭이 향할 앱 최상위 화면 (#1111).
 *
 * 알림 Intent extra 로 프로세스 경계를 넘나드는 값이라 [contractValue] 는 리팩터링에도 흔들리지
 * 않는 고정 문자열이다. enum 이름을 바꿔도 이 값은 유지해야 한다 — 이미 시스템 트레이에 떠 있는
 * 알림의 [android.app.PendingIntent] 는 옛 값을 담은 채 살아 있다.
 *
 * 목적지 집합을 **최상위 화면으로 제한**한다. 알림은 앱 밖에서 오는 입력이라 어떤 화면이든 열 수
 * 있게 두면 인자·권한 관문을 우회하는 통로가 된다. 실제 `Route` 매핑은 app 모듈이 갖는다 —
 * `core:common` 은 화면 그래프를 알지 않는다(그래서 이 모듈의 알림 코드가 런처 Intent 로 우회한다).
 */
enum class NotificationDestination(
    val contractValue: String,
) {
    HOME("home"),
    MIND_RECORD("mind_record"),
    TIME_LETTER("time_letter"),
    AFTERNOTE("afternote"),
    ;

    companion object {
        private val valuesByContractValue = entries.associateBy(NotificationDestination::contractValue)

        /**
         * 계약 밖 값·공백·null 은 모두 `null` 이다. 폴백 화면을 무엇으로 둘지는 해석하는 쪽이
         * 정한다 — 이 함수는 「계약에 있는 값인가」만 판정한다.
         */
        fun fromContractValue(value: String?): NotificationDestination? =
            value
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let(valuesByContractValue::get)
    }
}
