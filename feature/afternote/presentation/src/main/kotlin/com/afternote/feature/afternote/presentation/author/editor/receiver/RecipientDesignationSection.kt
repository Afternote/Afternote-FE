package com.afternote.feature.afternote.presentation.author.editor.receiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.EditorSectionLabel
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection

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
            isRequired = true,
        )
        AfternoteEditorReceiverList(
            afternoteEditReceivers = section.afternoteEditReceivers,
            events = section.callbacks,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipientDesignationSectionPreview() {
    AfternoteTheme {
        RecipientDesignationSection(
            section =
                AfternoteEditorReceiverSection(
                    afternoteEditReceivers =
                        listOf(
                            AfternoteEditorReceiver(id = "1", name = "홍길동", label = "가족"),
                            AfternoteEditorReceiver(id = "2", name = "김철수", label = "친구"),
                        ),
                ),
        )
    }
}
