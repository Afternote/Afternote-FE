package com.afternote.core.ui.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Red

private val TextFieldShape = RoundedCornerShape(8.dp)
private val TextFieldHeight = 56.dp

private const val PASSWORD_MASK_CHAR = '\u2022'

/**
 * 비밀번호 마스킹용 OutputTransformation.
 * [AfternoteTextField]에서 keyboardType이 [KeyboardType.Password]일 때 사용합니다.
 */
val PasswordMaskTransformation =
    OutputTransformation {
        val originalLength = length
        replace(0, originalLength, PASSWORD_MASK_CHAR.toString().repeat(originalLength))
    }

/**
 * Afternote 디자인 시스템 단일 라인 텍스트 필드.
 *
 * Foundation [BasicTextField] + decorator 기반으로 Material3 강제 패딩 없이
 * 디자인 시안을 픽셀 단위로 제어합니다.
 */
@Composable
fun AfternoteTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    prefix: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    containerColor: Color? = null,
    height: Dp = TextFieldHeight,
    labelSpacing: Dp = 6.dp,
    focusRequester: FocusRequester? = null,
    /** [imeAction]으로 지정한 IME 액션(예: Done)이 눌릴 때. State 기반 [BasicTextField]의 [onKeyboardAction]에 연결됩니다. */
    onImeAction: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor = containerColor ?: AfternoteDesign.colors.white

    val borderColor =
        when {
            isError -> Red
            isFocused -> AfternoteDesign.colors.gray9
            containerColor != null -> Color.Transparent
            else -> AfternoteDesign.colors.gray4
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(labelSpacing),
    ) {
        if (label != null) {
            Text(
                text = label,
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        fontWeight = FontWeight.Normal,
                    ),
                color = AfternoteDesign.colors.gray9,
            )
        }

        BasicTextField(
            state = state,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(bgColor, TextFieldShape)
                    .border(1.dp, borderColor, TextFieldShape)
                    .then(
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                    ),
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                ),
            onKeyboardAction =
                if (onImeAction != null) {
                    { onImeAction.invoke() }
                } else {
                    null
                },
            inputTransformation = inputTransformation,
            outputTransformation = outputTransformation,
            interactionSource = interactionSource,
            enabled = enabled,
            textStyle =
                AfternoteDesign.typography.textField.copy(
                    color = AfternoteDesign.colors.gray9,
                ),
            cursorBrush = SolidColor(AfternoteDesign.colors.gray9),
            decorator = { innerTextField ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (prefix != null) {
                        prefix()
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.text.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = AfternoteDesign.typography.textField,
                                color = AfternoteDesign.colors.gray4,
                            )
                        }
                        innerTextField()
                    }

                    if (trailingContent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        trailingContent()
                    }
                }
            },
        )
    }
}

// ============================================================================
// Previews
// ============================================================================

@Preview(showBackground = true, name = "기본 플레이스홀더")
@Composable
private fun AfternoteTextFieldBasicPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AfternoteTextField(
                state = rememberTextFieldState(),
                placeholder = "Text Field",
            )
            AfternoteTextField(
                state = rememberTextFieldState("입력된 텍스트"),
                placeholder = "Text Field",
            )
        }
    }
}

@Preview(showBackground = true, name = "라벨 + 에러")
@Composable
private fun AfternoteTextFieldLabeledPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AfternoteTextField(
                state = rememberTextFieldState(),
                label = "아이디",
                placeholder = "Text Field",
            )
            AfternoteTextField(
                state = rememberTextFieldState(),
                label = "비밀번호",
                placeholder = "Text Field",
                keyboardType = KeyboardType.Password,
                isError = true,
            )
        }
    }
}

@Preview(showBackground = true, name = "컨테이너 색상")
@Composable
private fun AfternoteTextFieldFilledPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AfternoteTextField(
                state = rememberTextFieldState(),
                label = "서비스명",
                placeholder = "Text Field",
                containerColor = AfternoteDesign.colors.gray1,
            )
        }
    }
}
