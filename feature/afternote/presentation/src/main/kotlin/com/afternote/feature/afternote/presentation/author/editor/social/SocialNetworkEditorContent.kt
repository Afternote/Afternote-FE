package com.afternote.feature.afternote.presentation.author.editor.social

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.Label
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.account.AccountSection
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.author.editor.processing.OptionRadioCardContent
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodList
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodListParams
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection
import com.afternote.feature.afternote.presentation.shared.SelectableRadioCard

/**
 * 소셜네트워크 등 일반적인 종류 선택 시 표시되는 콘텐츠
 */
@Composable
fun SocialNetworkEditorContent(
    modifier: Modifier = Modifier,
    params: SocialNetworkEditorContentParams,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 계정 정보 섹션
        Label(
            text = "계정 정보",
            isRequired = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "아이디",
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.height(6.dp))
        AfternoteTextField(
            state = params.accountSection.idState,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "비밀번호",
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.height(6.dp))
        AfternoteTextField(
            state = params.accountSection.passwordState,
            keyboardType = KeyboardType.Password,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 계정 처리 방법 섹션
        Label(
            text = "계정 처리 방법",
            isRequired = true,
        )

        Spacer(modifier = Modifier.height(24.dp))

        AccountProcessingMethod.entries.forEachIndexed { index, method ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            SelectableRadioCard(
                selected = params.accountSection.selectedMethod == method,
                onClick = { params.accountSection.onMethodSelected(method) },
                modifier = Modifier.fillMaxWidth(),
                content = {
                    OptionRadioCardContent(
                        option = method,
                        selected = params.accountSection.selectedMethod == method,
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        params.recipientSection?.let { section ->
            RecipientDesignationSection(section = section)
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 처리 방법 리스트 섹션
        Label(
            text = "처리 방법 리스트",
            isRequired = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

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
