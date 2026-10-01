package com.afternote.feature.setting.presentation.receiver

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/** 설정 수신자 목록의 조회 상태별 렌더 (#1281). 실제 0건 안내는 기존 관리 화면 그대로라 여기 두지 않는다. */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverListInitialLoadingScreenshot() {
    ReceiverListStateScreenshotContent(ReceiverListUiState(emptyList(), ReceiverListLoadState.Loading))
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverListLoadFailureScreenshot() {
    ReceiverListStateScreenshotContent(ReceiverListUiState(emptyList(), ReceiverListLoadState.Failure))
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverListRefreshFailureScreenshot() {
    ReceiverListStateScreenshotContent(ReceiverListUiState(previewReceivers, ReceiverListLoadState.RefreshFailure))
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverListSelectLoadFailureScreenshot() {
    ReceiverListStateScreenshotContent(
        uiState = ReceiverListUiState(emptyList(), ReceiverListLoadState.Failure),
        selectForDeliveryConditions = true,
    )
}

@Composable
private fun ReceiverListStateScreenshotContent(
    uiState: ReceiverListUiState,
    selectForDeliveryConditions: Boolean = false,
) {
    AfternoteTheme {
        ReceiverListRouteContent(
            uiState = uiState,
            selectForDeliveryConditions = selectForDeliveryConditions,
            onBackClick = {},
            onRetryClick = {},
            onConfirmClick = {},
            onReceiverClick = {},
            onRegisterClick = {},
        )
    }
}

private val previewReceivers =
    listOf(
        ReceiverListItem(receiverId = 1L, name = "김지은", relation = "딸"),
        ReceiverListItem(receiverId = 2L, name = "박경민", relation = "친구"),
        ReceiverListItem(receiverId = 3L, name = "이서준", relation = "아들"),
    )
