package com.afternote.feature.receiver.presentation.afternotelist

import androidx.annotation.StringRes
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.presentation.error.ErrorPayload
import com.afternote.feature.receiver.presentation.error.isDeliveryConditionNotMet
import com.afternote.feature.receiver.presentation.error.toErrorPayload

/**
 * 목록 로드 실패 중 **화면 처리가 갈리는 것**만 담는다.
 *
 * 여기 오르는 기준은 «사유를 안다» 가 아니라 «알면 화면이 달라진다» 다. 처리가 같은 실패를 타입으로
 * 가르면 소비처 없는 갈래가 쌓인다([ReceiverFailure] 가 #934 실측으로 경계하는 상황). 그래서
 * 5xx·기타 4xx 는 여기 없다 — 재시도가 유효하다는 점에서 일반 실패와 처리가 같다.
 */
internal sealed interface ReceiverAfternoteListError {
    /**
     * 발신자가 세운 전달 조건이 아직 충족되지 않았다(403 / 서버 code 2009).
     * 재시도로는 풀리지 않으므로 화면이 재시도 수단을 주지 않는다.
     */
    data class NotDeliverable(
        val payload: ErrorPayload,
    ) : ReceiverAfternoteListError

    /** 서버 응답 없이 전송 계층에서 끝났다. 재시도가 유효하고, 안내는 «연결을 확인하라» 로 갈린다. */
    data object NetworkUnavailable : ReceiverAfternoteListError
}

/**
 * 실패를 화면 처리로 옮긴다. **`null` 은 «갈라 그릴 것이 없다»** — 종전 목록 화면의 일반 에러
 * 경로에 그대로 남긴다는 뜻이지, 처리하지 못했다는 뜻이 아니다.
 *
 * @param fallbackRes 서버가 사유 문구를 주지 않았을 때 [NotDeliverable] 이 쓸 폴백 리소스.
 */
internal fun Throwable.toListError(
    @StringRes fallbackRes: Int,
): ReceiverAfternoteListError? =
    when {
        isDeliveryConditionNotMet() -> ReceiverAfternoteListError.NotDeliverable(toErrorPayload(fallbackRes))
        this is ReceiverFailure.NetworkUnavailable -> ReceiverAfternoteListError.NetworkUnavailable
        else -> null
    }
