package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.R as CoreUiR

/**
 * 작성 화면의 "수신자 설정하기" 에서 호출되는 수신자 다중 선택 바텀시트.
 *
 * `GET /users/receivers` 로 불러온 수신자 목록에서 기록을 전달할 대상을 고르고,
 * 선택 결과는 작성 API 의 `receiverIds` 로 전송된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientSelectBottomSheet(
    receivers: List<Receiver>,
    selectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checkedIds = remember { mutableStateSetOf<Long>().apply { addAll(selectedIds) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AfternoteDesign.colors.gray1,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "수신자 선택",
                style = AfternoteDesign.typography.bodyLargeB,
                color = AfternoteDesign.colors.gray9,
            )

            if (receivers.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "등록된 수신자가 없어요.\n설정에서 수신자를 먼저 등록해주세요.",
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray6,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(receivers, key = { it.receiverId }) { receiver ->
                        RecipientSelectRow(
                            receiver = receiver,
                            checked = receiver.receiverId in checkedIds,
                            onToggle = {
                                if (!checkedIds.add(receiver.receiverId)) {
                                    checkedIds.remove(receiver.receiverId)
                                }
                            },
                        )
                    }
                }
            }

            Button(
                onClick = { onConfirm(checkedIds.toSet()) },
                shape = RoundedCornerShape(6.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = AfternoteDesign.colors.gray9,
                        contentColor = AfternoteDesign.colors.white,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
            ) {
                Text(
                    text = "선택 완료",
                    style = AfternoteDesign.typography.bodySmallB,
                )
            }
        }
    }
}

@Composable
private fun RecipientSelectRow(
    receiver: Receiver,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipientCheckCircle(checked = checked)
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

@Composable
private fun RecipientCheckCircle(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(20.dp)
                .clip(CircleShape)
                .then(
                    if (checked) {
                        Modifier.background(AfternoteDesign.colors.gray9)
                    } else {
                        Modifier.border(width = 1.5.dp, color = AfternoteDesign.colors.gray4, shape = CircleShape)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_ic_check),
                contentDescription = null,
                tint = AfternoteDesign.colors.white,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipientSelectBottomSheetRowPreview() {
    AfternoteTheme {
        Column {
            RecipientSelectRow(
                receiver = Receiver(receiverId = 1L, name = "박채연", relation = "딸", authCode = ""),
                checked = true,
                onToggle = {},
            )
            RecipientSelectRow(
                receiver = Receiver(receiverId = 2L, name = "김민수", relation = "아들", authCode = ""),
                checked = false,
                onToggle = {},
            )
        }
    }
}
