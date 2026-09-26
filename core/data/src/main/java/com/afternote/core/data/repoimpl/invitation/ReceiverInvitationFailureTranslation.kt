package com.afternote.core.data.repoimpl.invitation

import com.afternote.core.domain.error.ReceiverInvitationFailure
import com.afternote.core.network.model.ApiException
import java.io.IOException

/**
 * 초대 API 실패를 [ReceiverInvitationFailure] 로 옮긴다 (#944).
 *
 * BE `ErrorCode` 번호를 아는 것은 이 파일까지다. 판정 순서 —
 * 1. [IOException] — 서버에 닿지 못했다. 재시도 가능.
 * 2. 5xx — 등재 code 여도 장애다. 재시도 가능.
 * 3. 등재 code — code 만으로 사유가 확정된다. 서버 문구는 쓰지 않는다.
 * 4. 그 밖의 4xx — 사유 미확정, 재시도해도 같은 거절이라 재시도 불가로 둔다.
 *
 * 취소는 여기 오지 않는다 — 호출부의 `runCatchingCancellable` 이 먼저 되던진다.
 */
internal fun <T> Result<T>.mapReceiverInvitationFailure(): Result<T> {
    val original = exceptionOrNull() ?: return this
    return Result.failure(original.toReceiverInvitationFailure())
}

private fun Throwable.toReceiverInvitationFailure(): ReceiverInvitationFailure =
    when (this) {
        is ReceiverInvitationFailure -> this
        is IOException -> ReceiverInvitationFailure.Other(this, isRetryable = true)
        is ApiException -> toServerFailure()
        else -> ReceiverInvitationFailure.Other(this, isRetryable = false)
    }

private fun ApiException.toServerFailure(): ReceiverInvitationFailure =
    when {
        status >= SERVER_ERROR_STATUS_MIN -> ReceiverInvitationFailure.Other(this, isRetryable = true)
        code == CODE_UNAUTHENTICATED -> ReceiverInvitationFailure.Unauthenticated(this)
        code == CODE_INVITATION_NOT_FOUND -> ReceiverInvitationFailure.NotFound(this)
        code == CODE_INVITATION_EXPIRED -> ReceiverInvitationFailure.Expired(this)
        code == CODE_INVITATION_ACCEPTED_BY_OTHER -> ReceiverInvitationFailure.AcceptedByOther(this)
        code == CODE_INVITATION_SELF_ACCEPT -> ReceiverInvitationFailure.SelfAccept(this)
        code == CODE_RECEIVER_ALREADY_REGISTERED -> ReceiverInvitationFailure.AlreadyRegistered(this)
        else -> ReceiverInvitationFailure.Other(this, isRetryable = false)
    }

private const val SERVER_ERROR_STATUS_MIN = 500

private const val CODE_UNAUTHENTICATED = 1000
private const val CODE_INVITATION_NOT_FOUND = 1905
private const val CODE_INVITATION_EXPIRED = 1906
private const val CODE_INVITATION_ACCEPTED_BY_OTHER = 1907
private const val CODE_INVITATION_SELF_ACCEPT = 1908
private const val CODE_RECEIVER_ALREADY_REGISTERED = 1909
