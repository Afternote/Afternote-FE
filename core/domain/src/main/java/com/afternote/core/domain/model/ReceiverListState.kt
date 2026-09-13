package com.afternote.core.domain.model

import com.afternote.core.model.user.Receiver

/** 수신자 목록의 조회 결과. 캐시는 로그인 구간과 구독에 귀속된다. */
sealed interface ReceiverListState {
    /** null은 이 구독에서 아직 성공한 조회가 없다는 뜻이다. */
    data class Loading(
        val previousReceivers: List<Receiver>?,
    ) : ReceiverListState

    data class Success(
        val receivers: List<Receiver>,
    ) : ReceiverListState

    /** 401은 이전 목록을 폐기한다. 원래 실패는 데이터 계층의 ErrorReporter가 기록한다. */
    data class Failure(
        val previousReceivers: List<Receiver>?,
    ) : ReceiverListState

    data object SignedOut : ReceiverListState
}
