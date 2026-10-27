package com.afternote.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.domain.repository.notification.NotificationInboxItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.home.presentation.notification.NotificationInboxContent
import com.afternote.feature.home.presentation.notification.NotificationInboxUiState
import com.android.tools.screenshot.PreviewTest

/** Figma 「알람창」 — 알림 종류별 아이콘과 읽은 알림(alarm_check) (#2321). */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun notificationInboxScreenshot() {
    AfternoteTheme {
        NotificationInboxContent(
            uiState =
                NotificationInboxUiState(
                    isLoading = false,
                    items =
                        listOf(
                            item(1, "오늘의 질문이 도착했어요", "오늘의 질문에 답하고 마음을 기록해 볼까요?", "DAILY_QUESTION"),
                            item(2, "편지 전달까지 7일 남았어요", "김민지님께 예약하신 편지가 곧 전달돼요.", "TIME_LETTER_DETAIL"),
                            item(3, "대리인을 아직 지정하지 않으셨어요", "소중한 기록을 안전하게 관리할 수 있도록 대리인을 지정해 주세요.", "AGENT_SETTINGS"),
                            item(4, "보관 확인 절차가 진행 중이에요", "대리인의 확인 요청이 접수됐어요.\n앱에서 진행 상태를 확인하고 절차를 취소할 수 있어요.", "RETENTION_CONFIRMATION"),
                            item(5, "오늘의 질문이 도착했어요", "오늘의 질문에 답하고 마음을 기록해 볼까요?", "DAILY_QUESTION", isRead = true),
                        ),
                ),
            onBack = {},
            onRetry = {},
            onItemClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun notificationInboxEmptyScreenshot() {
    AfternoteTheme {
        NotificationInboxContent(
            uiState = NotificationInboxUiState(isLoading = false),
            onBack = {},
            onRetry = {},
            onItemClick = {},
        )
    }
}

private fun item(
    id: Long,
    title: String,
    body: String,
    destination: String,
    isRead: Boolean = false,
) = NotificationInboxItem(id, title, body, mapOf("destination" to destination), isRead)
