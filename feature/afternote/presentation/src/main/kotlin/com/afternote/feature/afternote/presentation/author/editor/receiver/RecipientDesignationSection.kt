package com.afternote.feature.afternote.presentation.author.editor.receiver
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.Label
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverCallbacks
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverSection

/**
 * 수신자 지정 섹션 (Figma 수신자 지정 컴포넌트).
 * 라벨 "수신자 지정"과 수신자 리스트를 표시합니다.
 */
@Composable
fun RecipientDesignationSection(
    modifier: Modifier = Modifier,
    section: AfternoteEditorReceiverSection,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Label(
            text = "수신자 추가",
            isRequired = true,
        )
        Spacer(modifier = Modifier.height(9.dp))
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
                    afternoteEditReceivers = emptyList(),
                    callbacks = AfternoteEditorReceiverCallbacks(),
                ),
        )
    }
}
