package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R

/**
 * 마음의 기록 카드의 ... 버튼에서 뜨는 액션 팝업.
 * Figma 노드 2249:14756 — 흰 배경 / 8dp radius / hard shadow / width 120dp / 항목 padding 16dp.
 */
@Composable
fun RecordActionPopup(
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = modifier.width(120.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 10.dp,
        ) {
            Column {
                PopupItem(
                    label = stringResource(R.string.mindrecord_record_action_delete),
                    onClick = onDelete,
                )
                PopupItem(
                    label = stringResource(R.string.mindrecord_record_action_edit),
                    onClick = onEdit,
                )
            }
        }
    }
}

@Composable
private fun PopupItem(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = AfternoteDesign.typography.bodyBase,
        color = AfternoteDesign.colors.gray9,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun RecordActionPopupPreview() {
    AfternoteTheme {
        Surface(
            modifier = Modifier.width(120.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 10.dp,
        ) {
            Column {
                PopupItem(label = "삭제하기", onClick = {})
                PopupItem(label = "수정하기", onClick = {})
            }
        }
    }
}
