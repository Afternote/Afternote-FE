package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.feature.afternote.domain.error.AfternoteFailure
import java.io.IOException

/**
 * 저장 API 실패를 presentation이 네트워크·서버 오류로 타입 분기할 수 있는 도메인 예외로 치환한다.
 */
internal fun mapAuthoringFailure(throwable: Throwable): Throwable {
    if (throwable is IOException) {
        return AfternoteFailure.NetworkUnavailable(throwable)
    }

    return throwable
}
