package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.setting.presentation.viewmodel.ReceiverListLoadState
import com.afternote.feature.setting.presentation.viewmodel.ReceiverListUiState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverListInitialLoadingScreenshot() {
    ReceiverListStateScreenshotContent(loadState = ReceiverListLoadState.Loading)
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverManageInitialFailureScreenshot() {
    ReceiverListStateScreenshotContent(loadState = ReceiverListLoadState.InitialFailure)
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverSelectInitialFailureScreenshot() {
    ReceiverListStateScreenshotContent(
        loadState = ReceiverListLoadState.InitialFailure,
        selectForDeliveryConditions = true,
    )
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverManageRefreshFailureScreenshot() {
    ReceiverListStateScreenshotContent(
        loadState = ReceiverListLoadState.RefreshFailure,
        receivers = receiverListScreenshotItems,
    )
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverSelectRefreshFailureScreenshot() {
    ReceiverListStateScreenshotContent(
        loadState = ReceiverListLoadState.RefreshFailure,
        selectForDeliveryConditions = true,
        receivers = receiverListScreenshotItems,
    )
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverManageReadyScreenshot() {
    ReceiverListStateScreenshotContent(
        loadState = ReceiverListLoadState.Ready,
        receivers = receiverListScreenshotItems,
    )
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverSelectReadyScreenshot() {
    ReceiverListStateScreenshotContent(
        loadState = ReceiverListLoadState.Ready,
        selectForDeliveryConditions = true,
        receivers = receiverListScreenshotItems,
    )
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC, fontScale = 1.5f)
@Composable
internal fun receiverSelectRefreshFailureLargeFontScreenshot() {
    ReceiverListStateScreenshotContent(
        loadState = ReceiverListLoadState.RefreshFailure,
        selectForDeliveryConditions = true,
        receivers = receiverListScreenshotItems,
    )
}

@Composable
private fun ReceiverListStateScreenshotContent(
    loadState: ReceiverListLoadState,
    selectForDeliveryConditions: Boolean = false,
    receivers: List<ReceiverListItem> = emptyList(),
) {
    AfternoteTheme {
        ReceiverListRouteContent(
            uiState = ReceiverListUiState(receivers = receivers, loadState = loadState),
            selectForDeliveryConditions = selectForDeliveryConditions,
            onBackClick = {},
            onRetry = {},
            onConfirmClick = {},
            onReceiverClick = {},
        )
    }
}

private val receiverListScreenshotItems =
    listOf(
        ReceiverListItem(receiverId = 1L, name = "김서연", relation = "가족"),
        ReceiverListItem(receiverId = 2L, name = "박지훈", relation = "친구"),
        ReceiverListItem(receiverId = 3L, name = "이하늘", relation = "연인"),
    )
