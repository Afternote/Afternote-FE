package com.afternote.feature.receiver.presentation.invitation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopupHost

/** [ReceiverInvitationLandingScreen] 의 상태 없는 본문 — 프리뷰·스크린샷·Robolectric 진입점. */
@Composable
internal fun ReceiverInvitationLandingContent(
    state: ReceiverInvitationUiState,
    onIntent: (ReceiverInvitationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ReceiverErrorPopupHost(
        popup = state.errorPopup,
        onRetry = { onIntent(ReceiverInvitationIntent.Retry) },
        onDismiss = { onIntent(ReceiverInvitationIntent.DismissErrorPopup) },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AfternoteDesign.colors.gray1,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.receiver_invitation_top_bar_title),
                onBackClick = { onIntent(ReceiverInvitationIntent.Defer) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            when (val phase = state.phase) {
                ReceiverInvitationPhase.Loading -> {
                    LoadingBody(modifier = Modifier.weight(1f))
                }

                is ReceiverInvitationPhase.Ready -> {
                    ReadyBody(
                        inviterName = phase.inviterName,
                        modifier =
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                    )
                    AfternoteButton(
                        text = stringResource(R.string.receiver_invitation_accept),
                        onClick = { onIntent(ReceiverInvitationIntent.Accept) },
                        containerColor = KakaoContainerColor,
                        contentColor = KakaoContentColor,
                        isLoading = state.isAccepting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AfternoteButton(
                        text = stringResource(R.string.receiver_invitation_defer),
                        onClick = { onIntent(ReceiverInvitationIntent.Defer) },
                        type = AfternoteButtonType.Plain,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.receiver_invitation_privacy_caption, phase.inviterName),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray6,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is ReceiverInvitationPhase.Notice -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = phase.message.asString(),
                            style = AfternoteDesign.typography.bodyBase,
                            color = AfternoteDesign.colors.gray9,
                            textAlign = TextAlign.Center,
                        )
                    }
                    AfternoteButton(
                        text = stringResource(R.string.receiver_invitation_notice_confirm),
                        onClick = { onIntent(ReceiverInvitationIntent.AcknowledgeNotice) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBody(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AfternoteDesign.colors.gray6)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.receiver_invitation_loading),
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray6,
        )
    }
}

@Composable
private fun ReadyBody(
    inviterName: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.receiver_invitation_title, inviterName),
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.receiver_invitation_description, inviterName),
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray7,
        )
        Spacer(modifier = Modifier.height(48.dp))
        InviterRow(inviterName = inviterName)
    }
}

/** 시안의 정보 카드 한 줄 — 조회 응답에 있는 «나를 등록한 분» 만 그린다. */
@Composable
private fun InviterRow(inviterName: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AfternoteDesign.colors.white)
                .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.receiver_invitation_row_inviter),
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = inviterName,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

/** 카카오 브랜드 노랑 — 온보딩 로그인 버튼(`LoginScreen.KakaoContainerColor`)과 같은 값. */
internal val KakaoContainerColor = Color(0xFFFEE500)

/** 카카오 버튼 라벨 색 — 심볼과 같은 #212121·알파 90.2%. */
internal val KakaoContentColor = Color(0xE6212121)
