package com.afternote.core.common.result

import com.afternote.core.domain.result.runCatchingCancellable as domainRunCatchingCancellable

/**
 * 정본은 [com.afternote.core.domain.result.runCatchingCancellable] 이다. feature domain UseCase 도 쓸 수
 * 있도록 JVM 모듈로 옮겼고, 기존 호출처가 바뀌지 않도록 이 자리는 위임만 남긴다.
 */
@Deprecated(
    message = "core:domain 으로 옮겼다. com.afternote.core.domain.result.runCatchingCancellable 을 쓴다.",
    replaceWith =
        ReplaceWith(
            expression = "runCatchingCancellable(block)",
            imports = ["com.afternote.core.domain.result.runCatchingCancellable"],
        ),
)
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = domainRunCatchingCancellable(block)
