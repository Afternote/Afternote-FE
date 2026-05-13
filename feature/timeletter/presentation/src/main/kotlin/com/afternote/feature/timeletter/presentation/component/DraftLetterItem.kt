package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus

@Composable
fun DraftLetterItem(
    draft: TimeLetter,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    isSelected: Boolean = false,
    onToggle: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(78.dp)
                .then(if (isEditMode) Modifier.clickable { onToggle() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isEditMode) {
            AfternoteCircularCheckbox(
                state = if (isSelected) CheckboxState.Default else CheckboxState.None,
                onClick = { onToggle() },
                size = 20.dp,
            )
        }
        Column {
            Text("수신인 : ${if (draft.receiverIds.isEmpty()) "미지정" else "${draft.receiverIds.size}명"}")
            Text(draft.title ?: "제목 없음")
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("발송예정일: ${draft.sendAt ?: "-"}")
    }
}

@Preview(showBackground = true)
@Composable
private fun DraftLetterItemPrev() {
    DraftLetterItem(
        draft =
            TimeLetter(
                id = 1L,
                title = "미래의 나에게",
                content = null,
                sendAt = "2026-12-31",
                status = TimeLetterStatus.DRAFT,
                mediaList = emptyList(),
                receiverIds = listOf(1L),
                createdAt = null,
                updatedAt = null,
            ),
    )
}
