package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R

/**
 * 키보드 툴바의 링크 버튼에서 호출되는 "링크 추가하기" 바텀시트.
 *
 * Figma 노드 533:16183 (link variant) — 헤더(드래그 핸들 + 제목 + 완료 버튼) + URL 입력 필드.
 * 완료 시 [onConfirm] 으로 입력된 URL 을 전달한다 (호출부에서 에디터에 삽입).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by remember { mutableStateOf("") }

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
        LinkSheetContent(
            url = url,
            onUrlChange = { url = it },
            onConfirm = { onConfirm(url) },
            modifier = modifier,
        )
    }
}

@Composable
private fun LinkSheetContent(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 헤더 — 좌(52dp placeholder) / 가운데 제목 / 우(완료 버튼). 좌우 균형으로 제목이 가운데 정렬.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.width(52.dp))

            Text(
                text = stringResource(R.string.mindrecord_link_sheet_title),
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray6,
            )

            ConfirmPill(onClick = onConfirm)
        }

        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.mindrecord_link_sheet_placeholder),
                    style = AfternoteDesign.typography.bodyBase,
                    color = AfternoteDesign.colors.gray4,
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = AfternoteDesign.colors.gray2,
                    unfocusedIndicatorColor = AfternoteDesign.colors.gray2,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ConfirmPill(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(AfternoteDesign.colors.gray2)
                .border(BorderStroke(1.dp, AfternoteDesign.colors.gray4), CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(
            text = stringResource(R.string.mindrecord_link_sheet_confirm),
            style = AfternoteDesign.typography.captionLargeB,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LinkBottomSheetPreview() {
    AfternoteTheme {
        LinkSheetContent(
            url = "",
            onUrlChange = {},
            onConfirm = {},
        )
    }
}
