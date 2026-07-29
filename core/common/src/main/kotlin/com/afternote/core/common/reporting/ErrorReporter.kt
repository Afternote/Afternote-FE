package com.afternote.core.common.reporting

import kotlin.coroutines.cancellation.CancellationException

/**
 * 앱을 죽이지 않고 흡수되는 실패(handled 예외)를 개발자 텔레메트리로 남기는 창구.
 *
 * 크래시 리포팅의 자동 수집은 uncaught 예외만 잡는다. 로그인 실패처럼 `Result.failure` 로
 * 흡수되어 스낵바 한 줄로 끝나는 오류는 이 인터페이스를 거쳐 명시적으로 기록해야
 * 실기 QA 전까지 미검출로 남지 않는다.
 *
 * 구현은 app 모듈에만 두고 여기서는 추상만 노출한다 — presentation·data 레이어가
 * 크래시 리포팅 SDK 에 직접 의존하지 않게 하기 위함이다.
 *
 * 사용자 노출 문구와는 별개 층이다. 여기 기록된 값은 콘솔 전용이고,
 * 화면에 띄울 문구는 UI 가 따로 정한다.
 */
interface ErrorReporter {
    /**
     * 실패 [throwable] 을 non-fatal 로 기록한다. 단 코루틴 취소는 기록하지 않고 반환한다.
     *
     * 사용자가 스스로 취소한 흐름처럼 오류가 아닌 경로는 호출하지 않는다 — 기록되면
     * 실제 장애의 신호 대 잡음비만 떨어진다.
     *
     * 취소를 여기서 한 번 더 거르는 이유: 호출부가 넘기는 실패는 대개 `runCatching` 이 만든
     * `Result` 인데, `runCatching` 은 [CancellationException] 까지 잡아 `Result.failure` 로 만든다.
     * 그래서 화면 이탈로 스코프가 취소된 정상 경로가 호출부에는 실패로 보인다 — 호출부마다
     * 걸러 달라고 하면 새 계측 지점이 생길 때마다 빠진다. 정책이므로 창구에서 지킨다.
     *
     * @param attributes 이 실패 이벤트에만 붙는 컨텍스트. 어느 단계에서 깨졌는지 구분할
     *                   최소 정보만 담는다(예: `"stage" to "server_login"`).
     *                   개인정보·자격증명은 넣지 않는다 — 이메일·토큰·주민번호 금지.
     */
    fun recordFailure(
        throwable: Throwable,
        attributes: Map<String, String> = emptyMap(),
    ) {
        if (throwable is CancellationException) return
        writeFailure(throwable = throwable, attributes = attributes)
    }

    /**
     * 걸러진 실패를 실제 리포팅 백엔드에 쓴다. 구현이 채우는 건 이쪽이고,
     * 호출부는 정책을 태우는 [recordFailure] 만 쓴다.
     */
    fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    )
}
