package com.afternote.feature.home.presentation.receiver

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.home.presentation.R
import com.afternote.feature.home.presentation.receiver.component.AfternoteSection
import com.afternote.feature.home.presentation.receiver.component.MindRecordSection
import com.afternote.feature.home.presentation.receiver.component.SenderMessageHeroCard
import com.afternote.feature.home.presentation.receiver.component.TimeLetterSection
import com.afternote.feature.home.presentation.receiver.model.ReceiverDownloadState
import com.afternote.feature.home.presentation.receiver.model.ReceiverHomeUiState

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
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            // 수신자에게 유효한 헤더 액션이 없다 — 둘 다 그리지 않는다 (#613).
            //
            // 프로필 아이콘은 목적지가 없는 장식이었고, 톱니는 **회원 설정 화면**을 그대로 열었다.
            // 수신자는 로그인한 적이 없는 사용자(`X-Auth-Code` 기반)라 그 화면의 유일한 항목인
            // 「로그아웃」에 지울 세션이 없다. 수신자용 설정 계약이 생기기 전까지 새 화면이나
            // 가짜 로그아웃을 지어내지 않고 진입점을 내린다(#613 디자인 게이트 해제).
            HomeTopBar(
                showProfileIcon = false,
                onSettingClick = null,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            ReceiverHomeUiState.Loading -> { }

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
                    snackbarHostState = snackbarHostState,
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
        // senderName 가 blank 면 헤더·Hero 카드 모두 안 그림 — "故 님이 남기신 기록" 같은 깨진 문구 회피.
        if (state.senderName.isNotBlank()) {
            Text(
                text = stringResource(R.string.home_receiver_sender_record_title, state.senderName),
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
            text = stringResource(R.string.home_receiver_download_all_button),
            onClick = { onEvent(ReceiverHomeEvent.RequestDownload) },
            type = AfternoteButtonType.Default,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 내려받기 확인 팝업 + 결과 처리.
 *
 * 실패 안내는 잠정 Snackbar 다 — 서버 작업 실패의 정본은 #446 공통 에러 팝업이라 컴포넌트가
 * 나오면 그쪽으로 재정렬된다 (#1391, #713 전례). `showSnackbar` 는 표출이 끝날 때까지 suspend
 * 하므로 소비 이벤트는 표출 뒤에 보낸다 — 표출 중에는 [state] 가 Failed 로 유지돼 effect 가
 * 재시작되지 않고, 소비로 Idle 이 되면 이미 완료된 effect 만 정리된다 (#664 AddSongScreen 컨벤션).
 */
@Composable
private fun DownloadDialogHost(
    state: ReceiverDownloadState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ReceiverHomeEvent) -> Unit,
) {
    val currentOnEvent by rememberUpdatedState(onEvent)

    val showDialog =
        state is ReceiverDownloadState.Confirming || state is ReceiverDownloadState.InProgress
    if (showDialog) {
        Popup(
            type = PopupType.Variant2,
            message = stringResource(R.string.home_receiver_download_all_dialog_message),
            onConfirm = { onEvent(ReceiverHomeEvent.ConfirmDownload) },
            onDismiss = { onEvent(ReceiverHomeEvent.DismissDownload) },
            isLoading = state is ReceiverDownloadState.InProgress,
        )
    }

    when (state) {
        is ReceiverDownloadState.Failed -> {
            val message = stringResource(state.messageRes)
            LaunchedEffect(state) {
                snackbarHostState.showSnackbar(message = message, withDismissAction = true)
                currentOnEvent(ReceiverHomeEvent.ConsumeDownloadResult)
            }
        }

        ReceiverDownloadState.Done -> {
            LaunchedEffect(state) {
                currentOnEvent(ReceiverHomeEvent.ConsumeDownloadResult)
            }
        }

        ReceiverDownloadState.Idle,
        ReceiverDownloadState.Confirming,
        ReceiverDownloadState.InProgress,
        -> {
            // 이 셋은 여기서 할 일이 없다 — 위 `Popup` 이 따로 보고 있고, 소비할 결과도
            // 아직 없다. 종전에는 `else -> { }` 였는데, 그러면 상태가 하나 늘 때 새 상태도
            // 조용히 여기로 흘러 아무 일도 일어나지 않는다. 이 `when` 이 하는 일이 결과
            // 소비(`ConsumeDownloadResult`)라서, 소비되지 못한 상태는 화면을 그 자리에
            // 가둔다. 이름으로 적어 두면 상태가 늘 때 컴파일이 막는다 (#1767).
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
                text = stringResource(R.string.home_receiver_error_message),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray7,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.home_receiver_retry),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.gray9,
                )
            }
        }
    }
}
