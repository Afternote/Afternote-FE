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
     * 예외 원문도 여기서 버린다: [Throwable.message] 와 cause 체인에는 서버 응답 본문·OAuth
     * 오류 응답이 그대로 담겨 있을 수 있고, 그 안에 이메일이나 자격증명 조각이 섞이면 콘솔로
     * 유출된다(실제 로그인 실패 리포트에 서버 문구가 그대로 실린 것을 확인했다). 그래서
     * [redact] 로 타입·스택트레이스만 남기고, 버린 문구 대신 타입 이름을 속성으로 넘긴다.
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
        writeFailure(
            throwable = redact(throwable),
            attributes = attributes + throwable.typeAttributes(),
        )
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

/**
 * 문구를 버리고 타입·스택트레이스만 남긴 사본. 리포팅 백엔드에는 이 사본이 올라간다.
 *
 * 원본 클래스를 그대로 쓸 수 없어서(문구가 불변 필드다) 타입 이름을 message 자리에 옮겨
 * 콘솔에서 어떤 예외였는지 읽히게 하고, 스택트레이스는 원본 것을 복사해 발생 위치를 보존한다.
 * cause 는 잇지 않는다 — 원인 예외의 문구도 같은 위험을 갖기 때문이다(타입은 아래 속성으로 남는다).
 */
private class RedactedFailure(
    originalType: String,
) : Throwable(originalType)

private fun redact(throwable: Throwable): Throwable =
    RedactedFailure(throwable.javaClass.name).apply {
        stackTrace = throwable.stackTrace
    }

/**
 * 버려진 문구 대신 남기는 타입 정보. 콘솔에서 원인 구분·필터에 쓴다.
 *
 * `buildMap` 안에서 `javaClass` 를 그냥 쓰면 확장 리시버가 아니라 빌더 자신을 가리키므로
 * 값은 람다 밖에서 미리 꺼낸다.
 */
private fun Throwable.typeAttributes(): Map<String, String> {
    val type = javaClass.name
    val causeType = cause?.javaClass?.name
    return buildMap {
        put(KEY_ERROR_TYPE, type)
        causeType?.let { put(KEY_ERROR_CAUSE_TYPE, it) }
    }
}

private const val KEY_ERROR_TYPE = "error_type"
private const val KEY_ERROR_CAUSE_TYPE = "error_cause_type"
