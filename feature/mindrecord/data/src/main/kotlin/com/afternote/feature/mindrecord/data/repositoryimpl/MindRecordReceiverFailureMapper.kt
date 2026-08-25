package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.ApiException
import com.afternote.feature.mindrecord.domain.error.DeliveryNotReadyException

/** 전달 조건 미충족 — `403 {"code":2009}` (실서버 실측, 2026-08-23). */
private const val CODE_DELIVERY_CONDITION_NOT_MET = 2009

/**
 * 수신자 기록 조회 실패를 도메인 예외로 옮긴다 — presentation 이 `core:network` 를 모른 채
 * 타입만으로 분기하게 하는 것이 목적이다 (`mapAccountFailure` 와 같은 자리·같은 이유).
 *
 * 가르는 신호는 서버 봉투의 `code` 뿐이고 `message` 는 옮기지 않는다. 그 필드가 사용자
 * 노출용이라는 규정이 명세에 없다. 표시 문구는 화면이 자기 리소스로 갖는다.
 */
internal fun <T> Result<T>.mapReceiverFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is ApiException -> {
            when (exception.code) {
                CODE_DELIVERY_CONDITION_NOT_MET -> Result.failure(DeliveryNotReadyException(exception))
                else -> this
            }
        }

        else -> {
            this
        }
    }
