package com.afternote.feature.afternote.presentation.author.editor.account

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
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodListSection
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection

/**
 * 계정 기반 카테고리(소셜네트워크·비즈니스) 공용 에디터 콘텐츠 —
 * 계정 정보 + 수신자 지정 + 처리 방법 리스트 + 남기실 말씀 구조가 동일해 두 카테고리가 공유한다 (이슈 #467).
 */
@Composable
fun AccountEditorContent(
    editorMessages: List<EditorMessage>,
    accountSection: AccountSection,
    recipientSection: AfternoteEditorReceiverSection,
    modifier: Modifier = Modifier,
    onMessageRegisterClick: (EditorMessage) -> Unit = {},
    onMessageDeleteClick: (EditorMessage) -> Unit = {},
    onMessageAddClick: () -> Unit = {},
    processingMethodSection: ProcessingMethodSection = ProcessingMethodSection(),
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
                state = accountSection.idState,
            )

            CaptionLabeledTextField(
                label = stringResource(R.string.feature_afternote_detail_label_password),
                state = accountSection.passwordState,
                keyboardType = KeyboardType.Password,
            )
        }

        // 수신자 지정 섹션
        // 위치 근거 = 비즈니스 시안(Figma 700:38735): 계정 정보 다음·처리 방법 리스트 앞.
        // 소셜 시안엔 아직 수신자 섹션이 없어 같은 폼을 쓰는 비즈니스가 유일한 위치 근거다.
        // '정보 처리 방법' 라디오 존폐가 확정되면 라디오와 이 섹션의 상대 순서만 조정하면 되고,
        // '처리 방법 리스트 앞'은 불변. 저장 검증이 수신자 1인 이상을 요구하므로 UI 를 노출한다.
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

@Preview(showBackground = true)
@Composable
private fun AccountEditorContentPreview() {
    AfternoteTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            // 첫 번째 옵션 선택됨 (파란 테두리), 나머지는 선택 안 됨 (테두리 없음) 상태를 한 화면에 표시
            AccountEditorContent(
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
                    ),
                recipientSection =
                    AfternoteEditorReceiverSection(
                        afternoteEditReceivers =
                            listOf(
                                AfternoteEditorReceiver(id = "1", name = "홍길동", label = "가족"),
                            ),
                    ),
            )
        }
    }
}
