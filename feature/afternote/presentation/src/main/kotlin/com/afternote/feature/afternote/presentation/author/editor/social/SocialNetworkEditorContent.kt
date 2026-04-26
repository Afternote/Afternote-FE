package com.afternote.feature.afternote.presentation.author.editor.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.CaptionLabeledTextField
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.EditorSectionLabel
import com.afternote.feature.afternote.presentation.author.editor.account.AccountSection
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodList
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.shared.SelectableRadioCard

/**
 * 소셜네트워크 등 일반적인 종류 선택 시 표시되는 콘텐츠
 */
@Composable
fun SocialNetworkEditorContent(
    modifier: Modifier = Modifier,
    params: SocialNetworkEditorContentParams,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // 계정 정보 섹션
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditorSectionLabel(
                text = stringResource(R.string.afternote_editor_label_account_info),
                isRequired = true,
                style = AfternoteDesign.typography.textField,
                color = AfternoteDesign.colors.gray8,
            )

            CaptionLabeledTextField(
                label = stringResource(R.string.feature_afternote_detail_label_id),
                state = params.accountSection.idState,
            )

            CaptionLabeledTextField(
                label = stringResource(R.string.feature_afternote_detail_label_password),
                state = params.accountSection.passwordState,
                keyboardType = KeyboardType.Password,
            )
        }

        // 계정 처리 방법 섹션
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EditorSectionLabel(
                text = stringResource(R.string.afternote_editor_label_account_process_method),
                isRequired = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountProcessingMethod.entries.forEach { method ->
                    SelectableRadioCard(
                        title = method.title,
                        description = method.description,
                        selected = params.accountSection.selectedMethod == method,
                        onClick = { params.accountSection.onMethodSelected(method) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

//        if (params.recipientSection != null) {
//            RecipientDesignationSection(section = params.recipientSection)
//        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 처리 방법 리스트 섹션
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
private fun SocialNetworkEditorContentPreview() {
    AfternoteTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            // 첫 번째 옵션 선택됨 (파란 테두리), 나머지는 선택 안 됨 (테두리 없음) 상태를 한 화면에 표시
            SocialNetworkEditorContent(
                params =
                    SocialNetworkEditorContentParams(
                        editorMessages =
                            listOf(
                                EditorMessage(
                                    titleState = rememberTextFieldState("남긴말1"),
                                ),
                                EditorMessage(),
                            ),
                        accountSection =
                            AccountSection(
                                idState = rememberTextFieldState(),
                                passwordState = rememberTextFieldState(),
                                selectedMethod = AccountProcessingMethod.MEMORIAL_ACCOUNT,
                                onMethodSelected = {},
                            ),
                    ),
            )
        }
    }
}
