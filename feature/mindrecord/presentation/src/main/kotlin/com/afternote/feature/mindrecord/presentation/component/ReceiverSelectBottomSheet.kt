package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R

/**
 * 일기 작성 화면 수신자 행에서 뜨는 수신자 선택 바텀시트 (Figma 2671:17921 진입 흐름).
 *
 * `GET /users/receivers` 로 불러온 수신인 목록에서 다중 선택하며, 선택 결과는 일기 등록
 * payload 의 `receiverIds` 로 전송된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverSelectBottomSheet(
    receivers: List<Receiver>,
    selectedReceiverIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AfternoteDesign.colors.gray1,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCCCCCC)),
            )
        },
    ) {
        ReceiverSelectContent(
            receivers = receivers,
            selectedReceiverIds = selectedReceiverIds,
            onToggle = onToggle,
            onConfirm = onDismiss,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReceiverSelectContent(
    receivers: List<Receiver>,
    selectedReceiverIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.mindrecord_write_receiver_sheet_title),
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray6,
            )
        }

        if (receivers.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.mindrecord_write_receiver_empty),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        } else {
            LazyColumn {
                items(receivers, key = { it.receiverId }) { receiver ->
                    ReceiverRow(
                        receiver = receiver,
                        selected = receiver.receiverId in selectedReceiverIds,
                        onToggle = { onToggle(receiver.receiverId) },
                    )
                    HorizontalDivider(thickness = 1.dp, color = AfternoteDesign.colors.gray3)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = AfternoteDesign.colors.gray9,
                        contentColor = AfternoteDesign.colors.white,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.mindrecord_write_receiver_confirm),
                    style = AfternoteDesign.typography.bodySmallB,
                )
            }
        }
    }
}

@Composable
private fun ReceiverRow(
    receiver: Receiver,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AfternoteCircularCheckbox(
            state = if (selected) CheckboxState.Default else CheckboxState.None,
            size = 24.dp,
            onClick = onToggle,
        )
        Text(
            text = receiver.name,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
        Text(
            text = receiver.relation,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReceiverSelectContentPreview() {
    AfternoteTheme {
        ReceiverSelectContent(
            receivers =
                listOf(
                    Receiver(receiverId = 1, name = "김소희", relation = "딸", authCode = ""),
                    Receiver(receiverId = 2, name = "박채연", relation = "조카", authCode = ""),
                ),
            selectedReceiverIds = setOf(1L),
            onToggle = {},
            onConfirm = {},
        )
    }
}
