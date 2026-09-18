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

/**
 * 카카오톡 초대 랜딩 (시안 4996:40023, #944).
 *
 * @param isLoggedIn 앱 셸이 판정한 인증 상태. 로그인 전이면 «수락» 은 ViewModel 로 가지 않고
 *   [onLoginRequired] 로 나간다 — 토큰은 남아 있고, 로그인이 끝나면 셸이 이 화면을 다시 띄운다.
 */
@Composable
fun ReceiverInvitationLandingScreen(
    isLoggedIn: Boolean,
    viewModel: ReceiverInvitationViewModel,
    onAccepted: (inviterName: String) -> Unit,
    onOpenReceivedRecords: () -> Unit,
    onLoginRequired: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveSignal(
        signal = state.acceptedInviterName,
        consumed = ReceiverInvitationIntent.ConsumeAccepted,
        onIntent = viewModel::onIntent,
        onSignal = onAccepted,
    )
    ObserveSignal(
        signal = state.openReceivedRecords.takeIf { it },
        consumed = ReceiverInvitationIntent.ConsumeOpenReceivedRecords,
        onIntent = viewModel::onIntent,
    ) { onOpenReceivedRecords() }
    ObserveSignal(
        signal = state.loginRequired.takeIf { it },
        consumed = ReceiverInvitationIntent.ConsumeLoginRequired,
        onIntent = viewModel::onIntent,
    ) { onLoginRequired() }
    ObserveSignal(
        signal = state.close.takeIf { it },
        consumed = ReceiverInvitationIntent.ConsumeClose,
        onIntent = viewModel::onIntent,
    ) { onClose() }

    ReceiverInvitationLandingContent(
        state = state,
        onIntent = { intent ->
            if (intent == ReceiverInvitationIntent.Accept && !isLoggedIn) {
                onLoginRequired()
            } else {
                viewModel.onIntent(intent)
            }
        },
        modifier = modifier,
    )
}
