package com.afternote.core.common.reporting

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
     * 실패 [throwable] 을 non-fatal 로 기록한다.
     *
     * 사용자가 스스로 취소한 흐름처럼 오류가 아닌 경로는 호출하지 않는다 — 기록되면
     * 실제 장애의 신호 대 잡음비만 떨어진다.
     *
     * @param attributes 이 실패 이벤트에만 붙는 컨텍스트. 어느 단계에서 깨졌는지 구분할
     *                   최소 정보만 담는다(예: `"stage" to "server_login"`).
     *                   개인정보·자격증명은 넣지 않는다 — 이메일·토큰·주민번호 금지.
     */
    fun recordFailure(
        throwable: Throwable,
        attributes: Map<String, String> = emptyMap(),
    )
}
