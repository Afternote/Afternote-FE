package com.afternote.feature.afternote.presentation.receiver.afternotelist

import com.afternote.feature.receiver.domain.error.ReceiverFailure

/**
 * 목록 로드 실패 중 **화면 처리가 갈리는 것**만 담는다.
 *
 * 여기 오르는 기준은 «사유를 안다» 가 아니라 «알면 화면이 달라진다» 다. 처리가 같은 실패를 타입으로
 * 가르면 소비처 없는 갈래가 쌓인다([ReceiverFailure] 가 #934 실측으로 경계하는 상황). 그래서
 * 5xx·기타 4xx 는 여기 없다 — 재시도가 유효하다는 점에서 일반 실패와 처리가 같다.
 *
 * 갈래가 문구를 나르지 않는 것도 같은 이유다. 사유가 특정된 이상 표시 문구는 화면이 리소스로 가지면
 * 되고, 도메인·서버 문구를 여기까지 끌고 오면 갈래마다 그 통로를 다시 만들어야 한다.
 */
internal sealed interface ReceiverAfternoteListError {
    /**
     * 발신자가 세운 전달 조건이 아직 충족되지 않았다. 재시도로는 풀리지 않으므로 화면이 재시도
     * 수단을 주지 않는다. 어느 서버 사유였는지는 Data 계층이 이미 타입으로 해석했다.
     */
    data object NotDeliverable : ReceiverAfternoteListError

    /** 서버 응답 없이 전송 계층에서 끝났다. 재시도가 유효하고, 안내는 «연결을 확인하라» 로 갈린다. */
    data object NetworkUnavailable : ReceiverAfternoteListError
}

/**
 * 실패를 화면 처리로 옮긴다. **`null` 은 «갈라 그릴 것이 없다»** — 종전 목록 화면의 일반 에러
 * 경로에 그대로 남긴다는 뜻이지, 처리하지 못했다는 뜻이 아니다.
 */
internal fun Throwable.toListError(): ReceiverAfternoteListError? = (this as? ReceiverFailure)?.toListErrorOrNull()

/**
 * 루트로 좁혀 `when` 을 exhaustive 하게 만든다. 새 수신자 실패 유형이 생기면 목록 화면의 처리 여부도
 * 함께 정해야 하므로 컴파일러가 이 소비처를 잡아야 한다.
 */
private fun ReceiverFailure.toListErrorOrNull(): ReceiverAfternoteListError? =
    when (this) {
        is ReceiverFailure.DeliveryConditionNotMet -> ReceiverAfternoteListError.NotDeliverable

        is ReceiverFailure.NetworkUnavailable -> ReceiverAfternoteListError.NetworkUnavailable

        is ReceiverFailure.UnexpectedServerFailure,
        is ReceiverFailure.UserRejection,
        -> null
    }
