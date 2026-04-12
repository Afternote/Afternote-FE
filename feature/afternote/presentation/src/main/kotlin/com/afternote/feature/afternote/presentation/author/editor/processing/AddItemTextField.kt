package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 아이템 추가용 텍스트 필드 컴포넌트
 *
 * 포커스 해제 시 자동으로 아이템을 추가하고 텍스트 필드를 숨깁니다.
 */
@Composable
fun AddItemTextField(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onItemAdded: (String) -> Unit,
    onVisibilityChanged: (Boolean) -> Unit,
    placeholder: String = "Text Field",
) {
    val textFieldState = rememberTextFieldState()
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

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

    LaunchedEffect(visible, isFocused) {
        if (visible && wasFocused && !isFocused) {
            addItemIfNotEmpty()
        }
        wasFocused = isFocused
    }

    if (visible) {
        Spacer(modifier = Modifier.height(7.dp))
        // 텍스트 필드 컨테이너 (하단 보더 포함)
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .bottomBorder(color = AfternoteDesign.colors.gray2, width = 1.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            BasicTextField(
                state = textFieldState,
                modifier = Modifier.fillMaxWidth().height(24.dp),
                lineLimits = TextFieldLineLimits.SingleLine,
                interactionSource = interactionSource,
                textStyle =
                    AfternoteDesign.typography.bodyBase.copy(
                        lineHeight = 20.sp,
                        color = AfternoteDesign.colors.gray9,
                    ),
                cursorBrush = SolidColor(AfternoteDesign.colors.black),
                decorator = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (textFieldState.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style =
                                    AfternoteDesign.typography.bodyBase.copy(
                                        lineHeight = 20.sp,
                                    ),
                                color = AfternoteDesign.colors.gray4,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddItemTextFieldPreview() {
    AfternoteTheme {
        AddItemTextField(
            visible = true,
            onItemAdded = {},
            onVisibilityChanged = {},
        )
    }
}
