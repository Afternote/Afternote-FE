package com.afternote.core.common.result

import kotlin.coroutines.cancellation.CancellationException

/**
 * suspend 호출을 감싸 [Result] 로 돌려주되, [CancellationException] 만은 삼키지 않고 다시 던진다.
 *
 * suspend 경계에서 stdlib `runCatching` 을 쓰면 안 되는 이유 — 그 함수는 "catching any Throwable"
 * 이라(https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/run-catching.html) 코루틴 취소까지
 * `Result.failure` 로 바꾼다. 그러면 취소가 상위로 전파되지 못하고, 이미 취소된 코루틴에서 호출부의
 * `onFailure` 갈래(UI 상태 갱신·스낵바 안내 등)가 실행된다.
 *
 * Android 코루틴 모범 사례가 이 소비를 직접 금지한다 — "To enable coroutine cancellation, don't
 * consume exceptions of type CancellationException (don't catch them, or always rethrow them if
 * caught)" (https://developer.android.com/kotlin/coroutines/coroutines-best-practices).
 *
 * 같은 역할을 stdlib 이 제공하지 않아 여기 둔다: cancellation-aware `runCatching` 요청은
 * kotlinx.coroutines #1814 로 2020-02-18 에 올라와 아직 열려 있다
 * (https://github.com/Kotlin/kotlinx.coroutines/issues/1814).
 *
 * 쓰는 자리는 suspend 를 감싸는 경계다. 취소가 없는 동기 코드에는 stdlib `runCatching` 을 그대로
 * 쓴다 — `inline` 이라 동기 코드에서도 호출은 되지만(그 경우 stdlib 과 동작이 같다) 이름값을 하지
 * 못한다.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
