package com.afternote.feature.afternote.presentation.author.editor.gallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.Label
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.author.editor.model.InfoMethodSection
import com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.OptionRadioCardContent
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodList
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodListParams
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection
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
    Column(modifier = modifier.fillMaxWidth()) {
        // 계정 처리 방법 섹션
        Label(
            text = stringResource(R.string.afternote_editor_label_account_process_method),
            isRequired = true,
        )

        Spacer(modifier = Modifier.height(24.dp))

        InformationProcessingMethod.entries.forEachIndexed { index, method ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            SelectableRadioCard(
                selected = params.infoMethodSection.selectedMethod == method,
                onClick = { params.infoMethodSection.onMethodSelected(method) },
                modifier = Modifier.fillMaxWidth(),
                content = {
                    OptionRadioCardContent(
                        option = method,
                        selected = params.infoMethodSection.selectedMethod == method,
                    )
                },
            )
        }

        if (params.infoMethodSection.selectedMethod ==
            InformationProcessingMethod.TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            RecipientDesignationSection(section = params.recipientSection)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 처리 방법 리스트 섹션
        Label(
            text = stringResource(R.string.afternote_editor_label_process_method_list),
            isRequired = true,
        )

        Spacer(modifier = Modifier.height(20.dp))

        ProcessingMethodList(
            params =
                ProcessingMethodListParams(
                    items = params.processingMethodSection.items,
                    onItemDeleteClick = params.processingMethodSection.callbacks.onItemDeleteClick,
                    onItemAdded = params.processingMethodSection.callbacks.onItemAdded,
                    onTextFieldVisibilityChanged = params.processingMethodSection.callbacks.onTextFieldVisibilityChanged,
                    onItemEdited = params.processingMethodSection.callbacks.onItemEdited,
                ),
        )

        Spacer(modifier = Modifier.height(32.dp))

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
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            // 첫 번째 옵션 선택됨 (파란 테두리), 두 번째는 선택 안 됨 (테두리 없음) 상태를 한 화면에 표시
            GalleryAndFileEditorContent(
                params =
                    GalleryAndFileEditorContentParams(
                        editorMessages = listOf(EditorMessage()),
                        infoMethodSection =
                            InfoMethodSection(
                                selectedMethod = InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER,
                                onMethodSelected = {},
                            ),
                        recipientSection = AfternoteEditorReceiverSection(),
                        processingMethodSection = ProcessingMethodSection(),
                    ),
            )
        }
    }
}

@Preview(showBackground = true, name = "추가 수신자 선택됨")
@Composable
private fun GalleryAndFileEditorContentWithAfternoteEditorReceiversPreview() {
    AfternoteTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            GalleryAndFileEditorContent(
                params =
                    GalleryAndFileEditorContentParams(
                        editorMessages = listOf(EditorMessage()),
                        infoMethodSection =
                            InfoMethodSection(
                                selectedMethod = InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER,
                                onMethodSelected = {},
                            ),
                        recipientSection =
                            AfternoteEditorReceiverSection(
                                afternoteEditReceivers = emptyList(),
                            ),
                    ),
            )
        }
    }
}
