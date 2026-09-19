package com.afternote.feature.receiver.presentation.invitation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.receiver.presentation.R
import com.afternote.core.ui.R as CoreR

/**
 * 초대 수락 완료 (시안 4996:39921, #944). «확인하기» 와 back 모두 받은 기록함으로 간다 —
 * 수락은 서버에 이미 반영됐으므로 되돌아갈 랜딩이 없다.
 */
@Composable
fun ReceiverInvitationCompleteScreen(
    inviterName: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AfternoteDesign.colors.gray1,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.receiver_invitation_complete_top_bar_title),
                onBackClick = onConfirm,
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(96.dp))
            SuccessCheck()
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.receiver_invitation_complete_title),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.gray9,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.receiver_invitation_complete_description, inviterName),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray7,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.receiver_invitation_complete_info),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AfternoteDesign.colors.gray2)
                        .padding(16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteButton(
                text = stringResource(R.string.receiver_invitation_complete_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 시안의 초록 체크 — 테마에 초록 토큰이 없어 시안 실측값을 이 화면 안에만 둔다. */
@Composable
private fun SuccessCheck() {
    Box(
        modifier =
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SuccessCircleColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreR.drawable.core_ui_ic_check),
            contentDescription = stringResource(R.string.receiver_invitation_complete_check_description),
            tint = SuccessCheckColor,
            modifier = Modifier.size(width = 36.dp, height = 27.dp),
        )
    }
}

private val SuccessCircleColor = Color(0xFFD5EBDB)
private val SuccessCheckColor = Color(0xFF2E9E4F)
