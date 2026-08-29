package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.feature.afternote.domain.error.AfternoteFailure
import java.io.IOException

/**
 * 저장 API 실패를 presentation 이 네트워크·서버 오류로 타입 분기할 수 있는 도메인 예외로 옮긴다
 * (`mapAccountFailure`·`mapReceiverFailure` 와 같은 자리·같은 이유).
 *
 * 서버가 응답하며 거절한 실패는 원본 그대로 흘려보낸다 — 저장 경로에서 화면 처리가 달라지는
 * 서버 사유는 현재 없다.
 *
 * 취소는 여기 오지 않는다 — 호출부가 전부 `runCatchingCancellable`(#661) 이라
 * `CancellationException` 이 [Result] 에 담긴 채로 도달하지 않는다.
 */
internal fun <T> Result<T>.mapAuthoringFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is IOException -> Result.failure(AfternoteFailure.NetworkUnavailable(exception))
        else -> this
    }
