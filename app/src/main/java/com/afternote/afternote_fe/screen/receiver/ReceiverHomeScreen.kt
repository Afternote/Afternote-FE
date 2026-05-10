package com.afternote.afternote_fe.screen.receiver

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.afternote_fe.R
import com.afternote.afternote_fe.screen.receiver.component.AfternoteSection
import com.afternote.afternote_fe.screen.receiver.component.MindRecordSection
import com.afternote.afternote_fe.screen.receiver.component.SenderMessageHeroCard
import com.afternote.afternote_fe.screen.receiver.component.TimeLetterSection
import com.afternote.afternote_fe.screen.receiver.model.AfternoteSourceIcon
import com.afternote.afternote_fe.screen.receiver.model.MindRecordSummary
import com.afternote.afternote_fe.screen.receiver.model.ReceiverDownloadState
import com.afternote.afternote_fe.screen.receiver.model.ReceiverHomeUiState
import com.afternote.afternote_fe.screen.receiver.model.SenderMessage
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.afternote.presentation.R as AfternoteFeatureR

/**
 * 수신자 홈 화면 — 한 마디 + 마음의 기록·타임레터·애프터노트 카드 + 모든 기록 내려받기 버튼.
 *
 * Stateless. 상태/이벤트는 [ReceiverHomeEntry]가 주입한다.
 */
@Composable
fun ReceiverHomeScreen(
    uiState: ReceiverHomeUiState,
    onEvent: (ReceiverHomeEvent) -> Unit,
    actions: ReceiverHomeActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = { HomeTopBar(onSettingClick = actions.onSettingClick) },
    ) { innerPadding ->
        when (uiState) {
            ReceiverHomeUiState.Loading -> Unit

            is ReceiverHomeUiState.Error -> {
                ErrorState(
                    paddingValues = innerPadding,
                    onRetry = { onEvent(ReceiverHomeEvent.Retry) },
                )
            }

            is ReceiverHomeUiState.Success -> {
                SuccessContent(
                    state = uiState,
                    paddingValues = innerPadding,
                    onEvent = onEvent,
                    actions = actions,
                )
                DownloadDialogHost(
                    state = uiState.download,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: ReceiverHomeUiState.Success,
    paddingValues: PaddingValues,
    onEvent: (ReceiverHomeEvent) -> Unit,
    actions: ReceiverHomeActions,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.receiver_home_sender_record_title, state.senderName),
            style = AfternoteDesign.typography.h2,
            color = AfternoteDesign.colors.gray9,
        )
        state.senderMessage?.let { message ->
            SenderMessageHeroCard(
                senderName = state.senderName,
                date = message.date,
                message = message.body,
            )
        }
        MindRecordSection(
            summary = state.mindRecord,
            onGoClick = actions.onNavigateToMindRecord,
        )
        TimeLetterSection(
            totalCount = state.timeLetterTotalCount,
            onGoClick = actions.onNavigateToTimeLetter,
        )
        AfternoteSection(
            totalCount = state.afternoteTotalCount,
            icons = state.afternoteIcons,
            onGoClick = actions.onNavigateToAfternote,
        )
        AfternoteButton(
            text = stringResource(R.string.receiver_home_download_all_button),
            onClick = { onEvent(ReceiverHomeEvent.RequestDownload) },
            type = AfternoteButtonType.Default,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DownloadDialogHost(
    state: ReceiverDownloadState,
    onEvent: (ReceiverHomeEvent) -> Unit,
) {
    val context = LocalContext.current
    val currentOnEvent by rememberUpdatedState(onEvent)

    val showDialog =
        state is ReceiverDownloadState.Confirming || state is ReceiverDownloadState.InProgress
    if (showDialog) {
        Popup(
            type = PopupType.Variant2,
            message = stringResource(R.string.receiver_home_download_all_dialog_message),
            onConfirm = { onEvent(ReceiverHomeEvent.ConfirmDownload) },
            onDismiss = { onEvent(ReceiverHomeEvent.DismissDownload) },
            isLoading = state is ReceiverDownloadState.InProgress,
        )
    }

    LaunchedEffect(state) {
        when (state) {
            is ReceiverDownloadState.Failed -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                currentOnEvent(ReceiverHomeEvent.ConsumeDownloadResult)
            }
            ReceiverDownloadState.Done -> {
                currentOnEvent(ReceiverHomeEvent.ConsumeDownloadResult)
            }
            else -> Unit
        }
    }
}

@Composable
private fun ErrorState(
    paddingValues: PaddingValues,
    onRetry: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_tab_error_message),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray7,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.home_tab_retry),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.gray9,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReceiverHomeScreenPreview() {
    AfternoteTheme {
        ReceiverHomeScreen(
            uiState =
                ReceiverHomeUiState.Success(
                    senderName = "박서연",
                    senderMessage =
                        SenderMessage(
                            date = "2026.04.04",
                            body = "내가 없어도 너의 시간이 멈추지 않고\n행복하게 흘러갔으면 좋겠어.\n하늘에서 지켜줄게. 너무 슬퍼하지마 ㅎㅎ",
                        ),
                    mindRecord =
                        MindRecordSummary(
                            totalCount = 150,
                            dailyQuestionCount = 18,
                            diaryCount = 18,
                            deepThoughtCount = 18,
                        ),
                    timeLetterTotalCount = 30,
                    afternoteTotalCount = 10,
                    afternoteIcons =
                        listOf(
                            AfternoteSourceIcon("INSTAGRAM", AfternoteFeatureR.drawable.feature_afternote_img_insta_pattern),
                            AfternoteSourceIcon("GALLERY", AfternoteFeatureR.drawable.feature_afternote_img_googlephoto_pattern),
                            AfternoteSourceIcon("NAVER_MAIL", AfternoteFeatureR.drawable.feature_afternote_img_naver_mail_pattern),
                            AfternoteSourceIcon("KAKAOTALK", AfternoteFeatureR.drawable.feature_afternote_img_kakaotalk_pattern),
                        ),
                ),
            onEvent = {},
            actions = ReceiverHomeActions.Noop,
        )
    }
}
