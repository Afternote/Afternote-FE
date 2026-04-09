package com.afternote.feature.afternote.presentation.author.editor.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.Label
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
            AfternoteTextField(
                state = message.titleState,
                label = "제목",
                placeholder = "Text Field",
            )

            Spacer(modifier = Modifier.height(10.dp))

            AfternoteTextField(
                state = message.contentState,
                label = "내용",
                placeholder = "Text Field",
                lineLimits = TextFieldLineLimits.MultiLine(),
                minHeight = 160.dp,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(all = 16.dp),
            )

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
