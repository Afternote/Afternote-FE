package com.afternote.feature.setting.presentation.receiver

import com.afternote.core.model.setting.ReceiverListItem

/** 설정 수신자 목록 화면 상태 (#1281). [receivers] 는 지금 화면에 그릴 행이다. */
data class ReceiverListUiState(
    val receivers: List<ReceiverListItem> = emptyList(),
    val loadState: ReceiverListLoadState = ReceiverListLoadState.Loading,
)

enum class ReceiverListLoadState {
    /** 조회 중. 행이 있으면 행을 그대로 두고, 없을 때만 목록 자리를 로딩이 대신한다. */
    Loading,

    /** 조회 성공. 행이 0개면 실제로 등록된 수신자가 없다는 뜻이다. */
    Ready,

    /**
     * 보여 줄 행 없이 실패했다. 첫 조회 실패, 401 로 이전 목록을 버린 경우, 직전 성공이 0건이던 경우다.
     * 목록 자리를 실패 안내가 대신한다.
     */
    Failure,

    /** 보이던 행은 남기고 다시 불러오기만 실패했다. */
    RefreshFailure,
}
