package com.afternote.feature.setting.presentation.receiver

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.loading.ListRefreshErrorBanner
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.receiver.ReceiverListLoadFailure

/**
 * 설정 수신자 목록 라우트의 조회 상태 분기 (#1281). 관리·선택 두 진입이 같은 규칙을 쓴다.
 *
 * 보여 줄 행이 없을 때만 목록 자리를 로딩이나 실패 안내로 바꾼다. 행이 있으면 조회 중에도, 다시 불러오기가
 * 실패해도 행과 검색·선택을 그대로 둔다. 0건 안내와 등록 버튼은 조회에 성공해 실제로 0건일 때만 나온다.
 *
 * 선택 진입은 공용 `ReceiverSelectScreen` 의 `listReplacement` 로만 넘긴다. 목록을 남긴 채 얹는 갱신 실패
 * 배너는 그 슬롯으로 표현할 수 없어 관리 진입에만 있다.
 */
@Composable
internal fun ReceiverListRouteContent(
    uiState: ReceiverListUiState,
    selectForDeliveryConditions: Boolean,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onConfirmClick: (ReceiverListItem) -> Unit,
    onReceiverClick: (Long) -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listReplacement: (@Composable () -> Unit)? =
        when {
            uiState.receivers.isNotEmpty() -> null
            uiState.loadState == ReceiverListLoadState.Loading -> ({ LoadingBody() })
            uiState.loadState == ReceiverListLoadState.Failure -> ({ ReceiverListLoadFailure(onRetryClick = onRetryClick) })
            else -> null
        }

    if (selectForDeliveryConditions) {
        ReceiverListScreen(
            receivers = uiState.receivers,
            onBackClick = onBackClick,
            onConfirmClick = onConfirmClick,
            modifier = modifier,
            listReplacement = listReplacement,
        )
    } else {
        ReceiverManageScreen(
            receivers = uiState.receivers,
            onBackClick = onBackClick,
            onReceiverClick = onReceiverClick,
            onRegisterClick = onRegisterClick,
            modifier = modifier,
            listReplacement = listReplacement,
            listHeader =
                if (uiState.loadState == ReceiverListLoadState.RefreshFailure) {
                    { ListRefreshErrorBanner(onRetry = onRetryClick) }
                } else {
                    null
                },
        )
    }
}
