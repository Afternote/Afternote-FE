package com.afternote.feature.setting.presentation.component

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

/**
 * «카카오톡으로 이동합니다.» 바텀시트 (시안 4996:39786, #944).
 *
 * @param receiverName 폼에 적힌 수신자 이름 — 본문 «{이름} 님에게 보낼 초대메세지를 …» 에 실린다.
 * @param errorMessage 초대 발급·공유 실패 안내. 시트를 닫지 않고 그 자리에서 보인다 — 다시 누르면 재시도다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceiverInviteSheet(
    receiverName: String,
    isSending: Boolean,
    errorMessage: UiText?,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AfternoteDesign.colors.gray1,
        modifier = modifier,
    ) {
        ReceiverInviteSheetContent(
            receiverName = receiverName,
            isSending = isSending,
            errorMessage = errorMessage,
            onSend = onSend,
            onDismiss = onDismiss,
        )
    }
}
