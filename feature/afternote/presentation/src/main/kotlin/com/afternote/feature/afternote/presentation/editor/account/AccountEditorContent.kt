package com.afternote.feature.afternote.presentation.editor.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.CaptionLabeledTextField
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.EditorSectionLabel
import com.afternote.feature.afternote.presentation.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.editor.message.LeaveMessageEditorItem
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodListSection
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.editor.receiver.RecipientDesignationSection

/**
 * 계정 기반 카테고리(소셜네트워크·비즈니스) 공용 에디터 콘텐츠 —
 * 계정 정보 + 수신자 지정 + 처리 방법 리스트 + 남기실 말씀 구조가 동일해 두 카테고리가 공유한다 (이슈 #467).
 */
@Composable
fun AccountEditorContent(
    editorMessages: List<LeaveMessageEditorItem>,
    accountSection: AccountSection,
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
                label = stringResource(R.string.afternote_detail_label_id),
                state = accountSection.idState,
                placeholder = stringResource(R.string.afternote_editor_account_id_placeholder),
            )

            CaptionLabeledTextField(
                label = stringResource(R.string.afternote_detail_label_password),
                state = accountSection.passwordState,
                placeholder = stringResource(R.string.afternote_editor_account_password_placeholder),
                keyboardType = KeyboardType.Password,
            )
        }

        // 수신자 지정 섹션
        // 위치 근거 = 비즈니스 시안 정본(Figma 4327:70468): 계정 정보 다음·처리 방법 리스트 앞.
        // 소셜 시안엔 아직 수신자 섹션이 없어 같은 폼을 쓰는 비즈니스가 유일한 위치 근거다.
        // '정보 처리 방법' 라디오 3택은 폐지로 확정됐다 (#494). 정본 프레임의 TEXT 노드 전수에
        // 라디오 문구가 0건이고, 순서는 종류 → 서비스명 → 계정 정보 → 수신자 추가 →
        // 처리 방법 리스트 → 남기실 말씀이다. 이 순서는 AccountEditorSectionOrderTest 가 고정한다.
        // 저장 검증이 수신자 1인 이상을 요구하므로 UI 를 노출하되, 라벨의 필수 점은 붙이지 않는다 —
        // 정본 시안에 필수 마크가 없다.
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
