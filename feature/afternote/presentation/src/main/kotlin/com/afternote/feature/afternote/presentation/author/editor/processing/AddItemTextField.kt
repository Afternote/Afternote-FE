package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 아이템 추가용 텍스트 필드 컴포넌트
 *
 * 포커스 해제 시 자동으로 아이템을 추가하고 텍스트 필드를 숨깁니다.
 * 입력 UI는 [AfternoteTextField] 기본([TextFieldType.Basic]) 스타일을 사용합니다.
 */
@Composable
fun AddItemTextField(
    modifier: Modifier = Modifier,
    onItemAdded: (String) -> Unit,
    onVisibilityChanged: (Boolean) -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    val focusManager = LocalFocusManager.current

    val onItemAddedState = rememberUpdatedState(onItemAdded)
    val onVisibilityChangedState = rememberUpdatedState(onVisibilityChanged)
    var wasFocused by remember { mutableStateOf(false) }

    fun addItemIfNotEmpty() {
        val text = textFieldState.text.toString().trim()
        if (text.isNotEmpty()) {
            onItemAddedState.value(text)
            textFieldState.edit { replace(0, length, "") }
        }
        onVisibilityChangedState.value(false)
        focusManager.clearFocus()
    }
    AfternoteTextField(
        state = textFieldState,
        type = TextFieldType.Basic,
        placeholder = "처리 방법을 입력해 주세요",
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    val now = focusState.isFocused
                    if (wasFocused && !now) {
                        addItemIfNotEmpty()
                    }
                    wasFocused = now
                },
    )
}

@Preview(showBackground = true)
@Composable
private fun AddItemTextFieldPreview() {
    AfternoteTheme {
        AddItemTextField(
            onItemAdded = {},
            onVisibilityChanged = {},
        )
    }
}
