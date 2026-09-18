package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.social.shareReceiverInvitationViaKakao
import kotlinx.coroutines.launch
import com.afternote.core.ui.R as CoreR

/** «초대를 보냈어요»(4996:39909) 본문 — [ReceiverRegisterScreen] 이 phase 로 그린다. «다시 보내기» 의 공유 호출은 그 화면이 갖는다. */
@Composable
internal fun ReceiverInviteSentContent(
    receiverName: String,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onOpenReceiverList: () -> Unit,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.receiver_invite_sent_top_bar_title),
                onBackClick = onBackClick,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(96.dp))
            Box(
                modifier =
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(SentCircleColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.core_ui_ic_check),
                    contentDescription = stringResource(R.string.receiver_invite_sent_check_description),
                    tint = SentCheckColor,
                    modifier = Modifier.size(width = 36.dp, height = 27.dp),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.receiver_invite_sent_title),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.gray9,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.receiver_invite_sent_description, receiverName),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray7,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
            AfternoteButton(
                text = stringResource(R.string.receiver_invite_sent_open_list),
                onClick = onOpenReceiverList,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            AfternoteButton(
                text = stringResource(R.string.receiver_invite_sent_resend),
                onClick = onResend,
                type = AfternoteButtonType.Plain,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.receiver_invite_sent_caption),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 시안의 초록 체크 — 테마에 초록 토큰이 없어 시안 실측값을 이 화면 안에만 둔다. */
private val SentCircleColor = Color(0xFFD5EBDB)
private val SentCheckColor = Color(0xFF2E9E4F)
