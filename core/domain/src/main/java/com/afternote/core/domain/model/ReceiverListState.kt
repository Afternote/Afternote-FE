package com.afternote.core.domain.model

import com.afternote.core.model.user.Receiver

/**
 * 수신자 목록 조회의 결과 (#2045).
 *
 * `UserReceiverRepository.receiverListFlow` 는 실패를 마지막 목록이나 빈 목록으로 낮추므로 조회 중, 실패,
 * 실제 0건이 같은 모양으로 들어온다. 이 타입은 그 셋을 가른다.
 *
 * 이전 목록([Loading.previousReceivers], [Failure.previousReceivers])은 같은 로그인 세션에서 이 구독이
 * 마지막으로 성공한 목록이다. 이 구독에서 아직 성공한 조회가 없거나 401 로 버렸으면 null 이다.
 */
sealed interface ReceiverListState {
    data class Loading(
        val previousReceivers: List<Receiver>?,
    ) : ReceiverListState

    data class Success(
        val receivers: List<Receiver>,
    ) : ReceiverListState

    /** 원래 오류는 데이터 계층이 ErrorReporter 로 남긴다. 구독 취소로 끊긴 조회는 이 값을 내지 않는다. */
    data class Failure(
        val previousReceivers: List<Receiver>?,
    ) : ReceiverListState

    /**
     * 로그인 세션이 없다. 구독 중에 세션이 바뀌어도 새 세션 조회 전에 이 값을 먼저 받는다. 중간 로그아웃
     * 방출이 저장소에서 합쳐져 사라진 경우라서다 (#2135). 받은 쪽은 이전 목록을 더 보여 주지 않는다.
     */
    data object SignedOut : ReceiverListState
}
