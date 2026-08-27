package com.afternote.feature.receiver.presentation.deliveryverification.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteFieldContainer
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentSlotState

/**
 * 증빙 서류 슬롯(designs 6·7·8) — 라벨 + 흰색 입력 박스 + 우측 검정 동그라미 첨부 버튼 (이슈 #215).
 *
 * 빈 상태: 좌측 placeholder "촬영 또는 파일 첨부", 우측 검정 + 버튼.
 * 첨부 완료: 좌측 파일명 (사망진단서.jpeg 등), 우측 동일 버튼 (재첨부 가능).
 * 업로드 중: 좌측 progress indicator + "업로드 중…".
 *
 * 박스 전체와 우측 버튼 모두 동일 콜백([onPickClick]) — 시안 상 두 영역 모두 BottomSheet 트리거.
 */
@Composable
fun DocumentSlotCard(
    title: String,
    slot: DocumentSlotState,
    onPickClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray9,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AfternoteFieldContainer(
                modifier = Modifier.weight(1f),
                onClick = onPickClick,
                enabled = !slot.isUploading,
            ) {
                when {
                    slot.isUploading -> UploadingLabel()
                    slot.fileUrl != null -> FilledLabel(displayName = slot.displayName.orEmpty())
                    else -> EmptyLabel()
                }
            }
            AttachIconButton(onClick = onPickClick, enabled = !slot.isUploading)
        }
    }
}

@Composable
private fun EmptyLabel() {
    Text(
        text = stringResource(R.string.receiver_verify_document_input_placeholder),
        style = AfternoteDesign.typography.textField,
        color = AfternoteDesign.colors.gray4,
    )
}

@Composable
private fun FilledLabel(displayName: String) {
    Text(
        text = displayName.ifBlank { stringResource(R.string.receiver_verify_document_input_placeholder) },
        style = AfternoteDesign.typography.textField,
        color = AfternoteDesign.colors.gray9,
    )
}

@Composable
private fun UploadingLabel() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = AfternoteDesign.colors.gray6,
        )
        Text(
            text = stringResource(R.string.receiver_verify_document_uploading),
            style = AfternoteDesign.typography.textField,
            color = AfternoteDesign.colors.gray6,
        )
    }
}

@Composable
private fun AttachIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(AfternoteDesign.colors.gray9)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_ic_link),
            contentDescription = stringResource(R.string.receiver_verify_document_input_label),
            tint = AfternoteDesign.colors.white,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentSlotCardEmptyPreview() {
    AfternoteTheme {
        DocumentSlotCard(
            title = "사망진단서 업로드",
            slot = DocumentSlotState(),
            onPickClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentSlotCardFilledPreview() {
    AfternoteTheme {
        DocumentSlotCard(
            title = "사망진단서 업로드",
            slot = DocumentSlotState(displayName = "사망진단서.jpeg", fileUrl = "https://example.com/x"),
            onPickClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
