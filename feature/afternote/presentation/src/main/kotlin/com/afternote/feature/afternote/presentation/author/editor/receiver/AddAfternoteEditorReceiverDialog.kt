package com.afternote.feature.afternote.presentation.author.editor.receiver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.PhoneNumberInputTransformation
import com.afternote.core.ui.PhoneNumberVisualTransformation
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.popup.AfternotePopupCardLayout
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.selection.DropdownMenuStyle
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdown

/**
 * 수신자와의 관계 드롭다운 (로컬 상태 사용)
 */
@Composable
private fun RelationshipDropdown(
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    menuStyle: DropdownMenuStyle,
) {
    var expanded by remember { mutableStateOf(false) }

    SelectionDropdown(
        label = stringResource(R.string.afternote_editor_label_receiver_relation),
        selectedValue = selectedValue,
        options = options,
        onValueSelected = onValueSelected,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        menuStyle = menuStyle,
    )
}

/**
 * 수신자 추가 다이얼로그 컴포넌트
 *
 * 피그마 디자인 기반:
 * - 헤더: "수신자 추가" 타이틀, 우측 상단 "추가하기" 버튼 (AfternoteDesign.colors.gray9 배경, 흰색 텍스트)
 * - 수신자 이름 입력 필드
 * - 수신자와의 관계 드롭다운
 * - 전화번호로 추가하기 입력 필드
 * - 연락처에서 추가하기 버튼 (AfternoteDesign.colors.gray9 배경)
 */
@Composable
fun AddAfternoteEditorReceiverDialog(
    afternoteEditReceiverNameState: TextFieldState,
    phoneNumberState: TextFieldState,
    relationshipSelectedValue: String,
    relationshipOptions: List<String>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onRelationshipSelected: (String) -> Unit = {},
    onImportContactsClick: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        AddAfternoteEditorReceiverDialogContent(
            afternoteEditReceiverNameState = afternoteEditReceiverNameState,
            phoneNumberState = phoneNumberState,
            relationshipSelectedValue = relationshipSelectedValue,
            relationshipOptions = relationshipOptions,
            onAddClick = onAddClick,
            onRelationshipSelected = onRelationshipSelected,
            onImportContactsClick = onImportContactsClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun AddAfternoteEditorReceiverDialogContent(
    afternoteEditReceiverNameState: TextFieldState,
    phoneNumberState: TextFieldState,
    relationshipSelectedValue: String,
    relationshipOptions: List<String>,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit = {},
    onRelationshipSelected: (String) -> Unit = {},
    onImportContactsClick: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    AfternotePopupCardLayout(
        modifier =
            modifier
                .fillMaxWidth()
                .addFocusCleaner(focusManager),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 헤더: 타이틀과 추가하기 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.afternote_editor_label_receiver_add),
                style = AfternoteDesign.typography.bodyLargeB,
                color = AfternoteDesign.colors.gray9,
            )

            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AfternoteDesign.colors.gray9)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                focusManager.clearFocus()
                                onAddClick()
                            },
                        ).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_button),
                    style =
                        AfternoteDesign.typography.captionLargeR.copy(
                            color = AfternoteDesign.colors.white,
                        ),
                )
            }
        }

        // 수신자 이름 입력 필드
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.afternote_editor_receiver_name_label),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
            )
            AfternoteTextField(
                state = afternoteEditReceiverNameState,
            )
        }

        // 수신자와의 관계 드롭다운
        RelationshipDropdown(
            selectedValue = relationshipSelectedValue,
            options = relationshipOptions,
            onValueSelected = onRelationshipSelected,
            menuStyle =
                DropdownMenuStyle(
                    menuOffset = 5.2.dp,
                    menuBackgroundColor = AfternoteDesign.colors.gray1,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ),
        )
        // 전화번호로 추가하기 입력 필드
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.afternote_editor_add_by_phone_label),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
            )
            AfternoteTextField(
                state = phoneNumberState,
                keyboardType = KeyboardType.Phone,
                inputTransformation = PhoneNumberInputTransformation,
                outputTransformation = PhoneNumberVisualTransformation,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.afternote_editor_contact_import_section),
                style =
                    AfternoteDesign.typography.bodySmallR.copy(
                        color = AfternoteDesign.colors.gray6,
                    ),
            )
            // 연락처에서 추가하기 버튼
            AfternoteButton(
                text = stringResource(R.string.afternote_editor_import_contacts_button),
                onClick = {
                    focusManager.clearFocus()
                    onImportContactsClick()
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddAfternoteEditorReceiverDialogPreview() {
    AfternoteTheme {
        AddAfternoteEditorReceiverDialogContent(
            afternoteEditReceiverNameState = rememberTextFieldState("홍길동"),
            phoneNumberState = rememberTextFieldState("01012345678"),
            relationshipSelectedValue = "친구",
            relationshipOptions = listOf("가족", "친구", "동료", "기타"),
        )
    }
}
