package com.afternote.feature.setting.presentation.receiver.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.UiText
import com.afternote.core.ui.asString
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R

/** [ReceiverInviteSheet] 의 본문 — 시트 래퍼 없이 그려 스크린샷 baseline 을 잡는다. */
@Composable
internal fun ReceiverInviteSheetContent(
    receiverName: String,
    isSending: Boolean,
    errorMessage: UiText?,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.setting_receiver_invite_sheet_title),
            style = AfternoteDesign.typography.h3,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.setting_receiver_invite_sheet_description, receiverName),
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray7,
        )
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage.asString(),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.error,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        AfternoteButton(
            text = stringResource(R.string.setting_receiver_invite_send),
            onClick = onSend,
            containerColor = KakaoContainerColor,
            contentColor = KakaoContentColor,
            isLoading = isSending,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        AfternoteButton(
            text = stringResource(R.string.setting_receiver_invite_send_later),
            onClick = onDismiss,
            type = AfternoteButtonType.Plain,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 카카오 브랜드 노랑 — 온보딩 로그인 버튼과 같은 값. 라벨은 [AfternoteButtonType.Default] 의 흰색 대신 검정이어야 한다. */
internal val KakaoContainerColor = Color(0xFFFEE500)

/** 카카오 버튼 라벨 색 — 심볼과 같은 #212121·알파 90.2%. */
internal val KakaoContentColor = Color(0xE6212121)
