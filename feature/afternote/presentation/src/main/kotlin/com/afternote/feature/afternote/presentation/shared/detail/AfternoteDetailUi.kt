package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Black
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray5
import com.afternote.core.ui.theme.Gray8
import com.afternote.core.ui.theme.Gray9
import com.afternote.core.ui.theme.Sansneo
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiver

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Gray2),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Gray5)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Gray9)
    }
}

@Composable
fun ProcessingMethodItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = Gray9,
    )
}

/**
 * 수신자 목록 카드. 애프터노트 상세 화면(갤러리/소셜/추모 가이드라인)에서 공통 사용.
 * 수신자가 없으면 아무것도 표시하지 않음.
 */
@Composable
fun ReceiversCard(
    receivers: List<AfternoteEditReceiver>,
    modifier: Modifier = Modifier,
) {
    if (receivers.isEmpty()) return

    InfoCard(
        modifier = modifier.fillMaxWidth(),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.afternote_detail_receivers_label),
                    style =
                        TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontFamily = Sansneo,
                            fontWeight = FontWeight.Medium,
                            color = Gray9,
                        ),
                )
                receivers.forEach { receiver ->
                    ReceiverDetailItem(receiver = receiver)
                }
            }
        },
    )
}

@Composable
private fun ReceiverDetailItem(
    receiver: AfternoteEditReceiver,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(58.dp)
                    .clip(CircleShape),
        ) {
            Image(
                painter = painterResource(R.drawable.img_recipient_profile),
                contentDescription = "프로필 사진",
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column {
            Text(
                text = receiver.name,
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = Sansneo,
                        fontWeight = FontWeight.Medium,
                        color = Black,
                    ),
            )
            Text(
                text = receiver.label,
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = Sansneo,
                        fontWeight = FontWeight.Normal,
                        color = Gray8,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReceiversCardPreview() {
    AfternoteTheme {
        ReceiversCard(
            receivers =
                listOf(
                    AfternoteEditReceiver(
                        id = "1",
                        name = "황규운",
                        label = "친구",
                    ),
                    AfternoteEditReceiver(
                        id = "2",
                        name = "김소희",
                        label = "가족",
                    ),
                ),
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    serviceName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("삭제") },
        text = { Text("\"$serviceName\" 기록을 삭제할까요?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("삭제")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}

@Composable
fun EditDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit = {},
    showEditItem: Boolean = true,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        if (showEditItem) {
            DropdownMenuItem(
                text = { Text("수정하기") },
                onClick = {
                    onDismissRequest()
                    onEditClick()
                },
            )
        }
        DropdownMenuItem(
            text = { Text("삭제하기") },
            onClick = {
                onDismissRequest()
                onDeleteClick()
            },
        )
    }
}
