package com.afternote.feature.afternote.presentation.author.editor.receiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.PhoneNumberInputTransformation
import com.afternote.core.ui.PhoneNumberVisualTransformation
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.popup.AfternotePopupCardLayout
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.selection.DropdownMenuStyle
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdown
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdownLabelParams
import com.afternote.feature.afternote.presentation.author.editor.selection.rememberSelectionDropdownState

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
    val dropdownState =
        rememberSelectionDropdownState()

    SelectionDropdown(
        labelParams =
            SelectionDropdownLabelParams(
                label = stringResource(R.string.afternote_editor_label_receiver_relation),
            ),
        selectedValue = selectedValue,
        options = options,
        onValueSelected = onValueSelected,
        menuStyle = menuStyle,
        state = dropdownState,
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
    modifier: Modifier = Modifier,
    params: AddAfternoteEditorReceiverDialogParams,
) {
    val focusManager = LocalFocusManager.current
    Dialog(
        onDismissRequest = params.callbacks.onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        AfternotePopupCardLayout(
            modifier =
                modifier
                    .fillMaxWidth()
                    .addFocusCleaner(focusManager),
        ) {
            // 헤더: 타이틀과 추가하기 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "수신자 추가",
                    style = AfternoteDesign.typography.bodyLargeB,
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        params.callbacks.onAddClick()
                    },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = AfternoteDesign.colors.gray9,
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                ) {
                    Text(
                        text = "추가하기",
                        style =
                            AfternoteDesign.typography.captionLargeR.copy(
                                fontWeight = FontWeight.Medium,
                                color = AfternoteDesign.colors.white,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 수신자 이름 입력 필드
            Column {
                Text(
                    text = "수신자 이름",
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray9,
                )
                Spacer(modifier = Modifier.height(6.dp))
                AfternoteTextField(
                    state = params.afternoteEditReceiverNameState,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 수신자와의 관계 드롭다운
            RelationshipDropdown(
                selectedValue = params.relationshipSelectedValue,
                options = params.relationshipOptions,
                onValueSelected = params.callbacks.onRelationshipSelected,
                menuStyle =
                    DropdownMenuStyle(
                        menuOffset = 5.2.dp,
                        menuBackgroundColor = AfternoteDesign.colors.gray1,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                    ),
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 전화번호로 추가하기 입력 필드
            Column {
                Text(
                    text = "전화번호로 추가하기",
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray9,
                )
                Spacer(modifier = Modifier.height(6.dp))
                AfternoteTextField(
                    state = params.phoneNumberState,
                    keyboardType = KeyboardType.Phone,
                    inputTransformation = PhoneNumberInputTransformation,
                    outputTransformation = PhoneNumberVisualTransformation,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "연락처에서 추가하기",
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        color = AfternoteDesign.colors.gray9,
                    ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 연락처에서 추가하기 버튼
            AfternoteButton(
                text = "연락처 가져오기",
                onClick = {
                    focusManager.clearFocus()
                    params.callbacks.onImportContactsClick()
                },
                type = AfternoteButtonType.Default,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddAfternoteEditorReceiverDialogPreview() {
    AfternoteTheme {
        AddAfternoteEditorReceiverDialog(
            params =
                AddAfternoteEditorReceiverDialogParams(
                    afternoteEditReceiverNameState = rememberTextFieldState(),
                    phoneNumberState = rememberTextFieldState(),
                    relationshipSelectedValue = "친구",
                    relationshipOptions = listOf("친구", "가족", "연인"),
                ),
        )
    }
}
