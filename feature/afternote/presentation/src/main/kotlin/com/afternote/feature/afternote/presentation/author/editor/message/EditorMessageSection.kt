package com.afternote.feature.afternote.presentation.author.editor.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.Label
import com.afternote.core.ui.TextFieldShort
import com.afternote.core.ui.bottomBorder
import com.afternote.core.ui.button.AddCircleButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

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
    messages: List<EditorMessage>,
    onRegisterClick: (EditorMessage) -> Unit,
    onDeleteClick: (EditorMessage) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier.fillMaxWidth()) {
        Label(text = "남기실 말씀")

        Spacer(modifier = Modifier.height(16.dp))

        messages.forEachIndexed { index, message ->
            EditorMessageItem(
                message = message,
                showDeleteButton = index > 0,
                onRegisterClick = { onRegisterClick(message) },
                onDeleteClick = { onDeleteClick(message) },
                focusManager = focusManager,
            )

            if (index < messages.lastIndex) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AddCircleButton(
            contentDescription = "남기실 말씀 추가",
            onClick = {
                focusManager.clearFocus()
                onAddClick()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun EditorMessageItem(
    message: EditorMessage,
    showDeleteButton: Boolean,
    onRegisterClick: () -> Unit,
    onDeleteClick: () -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .bottomBorder(color = AfternoteDesign.colors.gray2, width = 1.dp),
        ) {
            Text(
                text = "제목",
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray9,
            )
            Spacer(modifier = Modifier.height(6.dp))
            TextFieldShort(
                state = message.titleState,
                placeholder = "Text Field",
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "내용",
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray9,
            )
            Spacer(modifier = Modifier.height(6.dp))
            EditorMessageContentField(state = message.contentState)

            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            if (showDeleteButton) {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        onDeleteClick()
                    },
                ) {
                    Text(
                        text = "삭제",
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray5,
                    )
                }
            }
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onRegisterClick()
                },
            ) {
                Text(
                    text = "등록",
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray5,
                )
            }
        }
    }
}

/**
 * "남기실 말씀" 내용 입력 필드.
 *
 * 멀티라인·160dp 최소 높이·16dp 전방향 패딩·16dp radius는
 * 이 섹션 고유 사양이라 [TextFieldShort] 대신 [BasicTextField]로 직접 구현합니다.
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
                .defaultMinSize(minHeight = 160.dp)
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
                        .padding(all = 16.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = "Text Field",
                        style = AfternoteDesign.typography.textField,
                        color = AfternoteDesign.colors.gray4,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun EditorMessageSectionPreview() {
    AfternoteTheme {
        EditorMessageSection(
            messages =
                listOf(
                    EditorMessage(
                        titleState = rememberTextFieldState("남긴말1"),
                        contentState = rememberTextFieldState(),
                    ),
                    EditorMessage(
                        titleState = rememberTextFieldState(),
                        contentState = rememberTextFieldState(),
                    ),
                ),
            onRegisterClick = {},
            onDeleteClick = {},
            onAddClick = {},
        )
    }
}

@Preview(showBackground = true, name = "단일 메시지")
@Composable
private fun EditorMessageSectionSinglePreview() {
    AfternoteTheme {
        EditorMessageSection(
            messages =
                listOf(
                    EditorMessage(
                        titleState = rememberTextFieldState(),
                        contentState = rememberTextFieldState(),
                    ),
                ),
            onRegisterClick = {},
            onDeleteClick = {},
            onAddClick = {},
        )
    }
}
