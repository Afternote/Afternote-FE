package com.afternote.feature.afternote.presentation.author.editor.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodListSection
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection

/**
 * 갤러리 및 파일 선택 시 표시되는 콘텐츠
 */
@Composable
fun GalleryAndFileEditorContent(
    modifier: Modifier = Modifier,
    params: GalleryAndFileEditorContentParams,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        RecipientDesignationSection(section = params.recipientSection)

        // 처리 방법 리스트 섹션
        ProcessingMethodListSection(section = params.processingMethodSection)

        // 남기실 말씀
        EditorMessageSection(
            messages = params.editorMessages,
            onRegisterClick = params.onMessageRegisterClick,
            onDeleteClick = params.onMessageDeleteClick,
            onAddClick = params.onMessageAddClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryAndFileEditorContentPreview() {
    AfternoteTheme {
        GalleryAndFileEditorContent(
            params =
                GalleryAndFileEditorContentParams(
                    editorMessages =
                        listOf(
                            EditorMessage(
                                titleState = rememberTextFieldState("가족들에게"),
                                contentState = rememberTextFieldState("항상 고마워요."),
                            ),
                        ),
                    recipientSection =
                        AfternoteEditorReceiverSection(
                            afternoteEditReceivers =
                                listOf(
                                    AfternoteEditorReceiver(id = "1", name = "홍길동", label = "가족"),
                                ),
                        ),
                    processingMethodSection =
                        ProcessingMethodSection(
                            items =
                                listOf(
                                    ProcessingMethodItem(id = "1", text = "계정 삭제"),
                                    ProcessingMethodItem(id = "2", text = "게시글 백업"),
                                ),
                        ),
                ),
        )
    }
}
