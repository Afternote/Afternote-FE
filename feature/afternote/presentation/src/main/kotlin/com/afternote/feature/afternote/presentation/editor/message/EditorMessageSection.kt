package com.afternote.feature.afternote.presentation.editor.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.CaptionLabeledTextField
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.EditorSectionLabel

/**
 * 남기실 말씀 섹션 - 여러 개의 메시지를 추가/삭제/등록할 수 있는 컴포넌트
 *
 * 피그마 디자인 기반:
 * - "남기실 말씀" 라벨
 * - 각 메시지: 제목 + 내용 텍스트필드 + 구분선 + 등록/삭제 버튼
 * - 하단 (+) 추가 버튼
 */
@Composable
fun EditorMessageSection(
    messages: List<LeaveMessageEditorItem>,
    onRegisterClick: (LeaveMessageEditorItem) -> Unit,
    onDeleteClick: (LeaveMessageEditorItem) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        EditorSectionLabel(
            text = stringResource(R.string.afternote_editor_label_messages),
        )

        Spacer(modifier = Modifier.height(12.dp))

        messages.forEachIndexed { index, message ->
            key(message.id) {
                if (message.isRegistered) {
                    RegisteredEditorMessageItem(message = message)
                } else {
                    EditorMessageItem(
                        message = message,
                        onRegisterClick = { onRegisterClick(message) },
                        onDeleteClick = { onDeleteClick(message) },
                        focusManager = focusManager,
                    )
                }

                if (index < messages.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = AfternoteDesign.colors.gray2)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        PlusBadgeButton(
            contentDescription = stringResource(R.string.afternote_editor_message_add_content_description),
            onClick = {
                focusManager.clearFocus()
                onAddClick()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** 내부 등록을 마친 말씀을 입력 라벨·액션 없이 보여주는 읽기 전용 블록. */
@Composable
private fun RegisteredEditorMessageItem(
    message: LeaveMessageEditorItem,
    modifier: Modifier = Modifier,
) {
    val toggleDescription =
        stringResource(
            if (message.isBodyVisible) {
                R.string.afternote_editor_message_registered_collapse_content_description
            } else {
                R.string.afternote_editor_message_registered_expand_content_description
            },
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button, onClick = message::toggleBodyVisibility)
                    .semantics {
                        contentDescription = toggleDescription
                    },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message.titleState.text.toString(),
                style = AfternoteDesign.typography.textField,
                color = AfternoteDesign.colors.gray9,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.afternote_ic_dropdown_vector),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray8,
                modifier = Modifier.rotate(if (message.isBodyVisible) 180f else 0f),
            )
        }

        if (message.isBodyVisible) {
            Text(
                text = message.contentState.text.toString(),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray7,
            )
        }
    }
}

@Composable
private fun EditorMessageItem(
    message: LeaveMessageEditorItem,
    onRegisterClick: () -> Unit,
    onDeleteClick: () -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CaptionLabeledTextField(
                label = stringResource(R.string.afternote_editor_message_field_title),
                state = message.titleState,
                placeholder = stringResource(R.string.afternote_editor_message_title_placeholder),
            )

            Column(
                modifier = Modifier.semantics { isTraversalGroup = true },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.afternote_editor_message_field_body),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
                EditorMessageContentField(state = message.contentState)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.afternote_editor_message_action_delete),
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray6,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(role = Role.Button) {
                            focusManager.clearFocus()
                            onDeleteClick()
                        }.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Spacer(Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.afternote_editor_message_action_register),
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray6,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(role = Role.Button) {
                            focusManager.clearFocus()
                            onRegisterClick()
                        }.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * "남기실 말씀" 내용 입력 필드.
 *
 * 멀티라인·160dp 최소 높이·16dp 전방향 패딩·16dp radius는
 * 이 섹션 고유 사양이라 [com.afternote.core.ui.AfternoteTextField] 대신 [BasicTextField]로 직접 구현합니다.
 */
@Composable
private fun EditorMessageContentField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        state = state,
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 180.dp)
                .background(AfternoteDesign.colors.white, shape)
                .border(1.dp, AfternoteDesign.colors.gray2, shape),
        lineLimits = TextFieldLineLimits.MultiLine(),
        textStyle =
            AfternoteDesign.typography.textField.copy(
                color = AfternoteDesign.colors.gray9,
            ),
        cursorBrush = SolidColor(AfternoteDesign.colors.black),
        decorator = { innerTextField ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.afternote_editor_message_text_field_placeholder),
                        style = AfternoteDesign.typography.textField,
                        color = AfternoteDesign.colors.gray4,
                    )
                }
                innerTextField()
            }
        },
    )
}
