package com.afternote.feature.afternote.presentation.editor.receiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.EditorSectionLabel
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiverSection

/**
 * 수신자 지정 섹션 (Figma 수신자 지정 컴포넌트).
 * 라벨 "수신자 지정"과 수신자 리스트를 표시합니다.
 */
@Composable
fun RecipientDesignationSection(
    modifier: Modifier = Modifier,
    section: AfternoteEditorReceiverSection,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditorSectionLabel(
            text = stringResource(R.string.afternote_editor_label_receiver_add),
            isRequired = false,
        )
        AfternoteEditorReceiverList(
            afternoteEditReceivers = section.afternoteEditReceivers,
            onAddClick = section.onAddClick,
            onItemDeleteClick = section.onItemDeleteClick,
        )
    }
}
