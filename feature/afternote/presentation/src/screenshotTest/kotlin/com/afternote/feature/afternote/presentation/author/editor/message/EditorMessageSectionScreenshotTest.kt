package com.afternote.feature.afternote.presentation.author.editor.message

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun editorMessageSectionEmptyScreenshot() {
    AfternoteTheme {
        EditorMessageSection(
            messages = emptyList(),
            onRegisterClick = {},
            onDeleteClick = {},
            onAddClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun editorMessageSectionRegisteredCollapsedScreenshot() {
    AfternoteTheme {
        val messages =
            remember {
                listOf(
                    EditorMessage(
                        titleState = TextFieldState("남긴말1"),
                        contentState = TextFieldState("전하고 싶은 이야기를 적어둔 내용입니다."),
                        initialRegistered = true,
                    ),
                    EditorMessage(),
                )
            }
        EditorMessageSection(
            messages = messages,
            onRegisterClick = {},
            onDeleteClick = {},
            onAddClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun editorMessageSectionRegisteredExpandedScreenshot() {
    AfternoteTheme {
        val messages =
            remember {
                listOf(
                    EditorMessage(
                        titleState = TextFieldState("남긴말1"),
                        contentState = TextFieldState("전하고 싶은 이야기를 적어둔 내용입니다.\n두 번째 줄까지 펼쳐서 보여줍니다."),
                        initialRegistered = true,
                    ).apply { isExpanded = true },
                    EditorMessage(),
                )
            }
        EditorMessageSection(
            messages = messages,
            onRegisterClick = {},
            onDeleteClick = {},
            onAddClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
