package com.afternote.feature.afternote.presentation.author.editor.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.EditorSectionLabel
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.author.editor.model.InfoMethodSection
import com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodList
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.shared.SelectableRadioCard

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
        // 계정 처리 방법 섹션
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EditorSectionLabel(
                text = stringResource(R.string.afternote_editor_label_account_process_method),
                isRequired = true,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InformationProcessingMethod.entries.forEachIndexed { index, method ->
                    SelectableRadioCard(
                        title = method.title,
                        description = method.description,
                        selected = params.infoMethodSection.selectedMethod == method,
                        onClick = { params.infoMethodSection.onMethodSelected(method) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (params.infoMethodSection.selectedMethod ==
            InformationProcessingMethod.TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER
        ) {
            RecipientDesignationSection(section = params.recipientSection)
        }

        // 처리 방법 리스트 섹션
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditorSectionLabel(
                text = stringResource(R.string.afternote_editor_label_process_method_list),
                isRequired = true,
            )
            ProcessingMethodList(
                items = params.processingMethodSection.items,
                onItemAdded = params.processingMethodSection.callbacks.onItemAdded,
                onItemDeleteClick = params.processingMethodSection.callbacks.onItemDeleteClick,
                onItemEdited = params.processingMethodSection.callbacks.onItemEdited,
                onTextFieldVisibilityChanged = params.processingMethodSection.callbacks.onTextFieldVisibilityChanged,
            )
        }

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
                    infoMethodSection =
                        InfoMethodSection(
                            selectedMethod = InformationProcessingMethod.TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER,
                        ) {},
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
