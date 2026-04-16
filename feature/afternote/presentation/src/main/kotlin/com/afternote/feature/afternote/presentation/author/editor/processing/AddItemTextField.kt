package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

/**
 * 아이템 추가용 텍스트 필드 컴포넌트
 *
 * 포커스가 해제되거나 키보드에서 완료(Done) 액션이 들어오면 비어 있지 않을 때 항목을 추가하고,
 * 부모에 표시 종료를 알립니다. 입력 UI는 [AfternoteTextField] 기본([TextFieldType.Basic]) 스타일입니다.
 *
 * 등장 직후 [LocalSoftwareKeyboardController]로 키보드를 명시적으로 띄우고, 완료·포커스 해제 시 숨겨
 * 일부 단말·전환 직후 포커스만 잡히는 경우를 줄입니다.
 */
@Composable
fun AddItemTextField(
    onItemAdded: (String) -> Unit,
    onVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldState = rememberTextFieldState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var wasFocused by remember { mutableStateOf(false) }

    fun addItemIfNotEmpty() {
        val text = textFieldState.text.toString().trim()
        if (text.isNotEmpty()) {
            onItemAdded(text)
            textFieldState.edit { replace(0, length, "") }
        }
        onVisibilityChanged(false)
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    AfternoteTextField(
        state = textFieldState,
        type = TextFieldType.Basic,
        placeholder = stringResource(R.string.afternote_editor_processing_method_add_placeholder),
        imeAction = ImeAction.Done,
        onImeAction = { addItemIfNotEmpty() },
        focusRequester = focusRequester,
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
