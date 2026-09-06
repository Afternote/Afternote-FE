package com.afternote.feature.afternote.presentation.editor.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.editor.message.LeaveMessageEditorItem
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodListSection
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.editor.receiver.RecipientDesignationSection

/**
 * 갤러리 및 파일 선택 시 표시되는 콘텐츠
 */
@Composable
fun GalleryAndFileEditorContent(
    editorMessages: List<LeaveMessageEditorItem>,
    recipientSection: AfternoteEditorReceiverSection,
    processingMethodSection: ProcessingMethodSection,
    modifier: Modifier = Modifier,
    onMessageRegisterClick: (LeaveMessageEditorItem) -> Unit,
    onMessageDeleteClick: (LeaveMessageEditorItem) -> Unit,
    onMessageAddClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        RecipientDesignationSection(section = recipientSection)

        // 처리 방법 리스트 섹션
        ProcessingMethodListSection(section = processingMethodSection)

        // 남기실 말씀
        EditorMessageSection(
            messages = editorMessages,
            onRegisterClick = onMessageRegisterClick,
            onDeleteClick = onMessageDeleteClick,
            onAddClick = onMessageAddClick,
        )
    }
}
