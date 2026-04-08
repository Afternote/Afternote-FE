package com.afternote.core.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Red

private const val PASSWORD_MASK_CHAR = '\u2022'

private val OutlineTextFieldShape = RoundedCornerShape(8.dp)
private val OutlineTextFieldHeightBasic = 70.dp
private val OutlineTextFieldHeightLabeled = 56.dp
private val OutlineTextFieldHeightMultiline = 160.dp

/**
 * Shared placeholder content for outline text fields.
 */
@Composable
private fun OutlineTextFieldPlaceholder(text: String) {
    Text(
        text = text,
        style = AfternoteDesign.typography.textField,
        color = AfternoteDesign.colors.gray4,
    )
}

@Composable
private fun outlineTextFieldBasicColors() =
    OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = AfternoteDesign.colors.gray4,
        disabledTextColor = AfternoteDesign.colors.gray9,
        disabledPlaceholderColor = AfternoteDesign.colors.gray4,
        disabledContainerColor = AfternoteDesign.colors.white,
    )

@Composable
private fun outlineTextFieldBasicColorsSimple() =
    OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = AfternoteDesign.colors.gray4,
    )

@Composable
private fun outlineTextFieldFilledColors(containerColor: Color) =
    OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Color.Transparent,
        focusedBorderColor = Color.Transparent,
        unfocusedContainerColor = containerColor,
        focusedContainerColor = containerColor,
    )

@Composable
private fun outlineTextFieldFilledColorsAll(containerColor: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = Color.Transparent,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = containerColor,
    )

/**
 * Style configuration for labeled OutlineTextField.
 */
data class LabeledTextFieldStyle(
    val containerColor: Color? = null,
    val labelSpacing: Dp = 6.dp,
    val labelFontSize: TextUnit = 12.sp,
    val labelLineHeight: TextUnit = 18.sp,
    val labelFontWeight: FontWeight = FontWeight.Normal,
    val labelColor: Color? = null,
)

private val PasswordOutputTransformation =
    OutputTransformation {
        val originalLength = length
        replace(0, originalLength, PASSWORD_MASK_CHAR.toString().repeat(originalLength))
    }

// ============================================================================
// Basic Variants (placeholder inside field, 70.dp height)
// ============================================================================

/**
 * Single-line outlined text field (basic variant).
 *
 * @param textFieldState Text field state holder.
 * @param label Placeholder text shown when empty.
 * @param keyboardType Keyboard type; if [KeyboardType.Password], input is visually masked.
 * @param enabled Whether the field is enabled.
 * @param focusRequester Optional focus controller for programmatic focus.
 * @param requestFocusOnEnabled If true, requests focus when the field is laid out and enabled.
 */
@Composable
fun OutlineTextField(
    textFieldState: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    requestFocusOnEnabled: Boolean = false,
) {
    OutlinedTextField(
        state = textFieldState,
        lineLimits = TextFieldLineLimits.SingleLine,
        placeholder = { OutlineTextFieldPlaceholder(text = label) },
        colors = outlineTextFieldBasicColors(),
        shape = OutlineTextFieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        outputTransformation =
            if (keyboardType == KeyboardType.Password) {
                PasswordOutputTransformation
            } else {
                null
            },
        enabled = enabled,
        readOnly = false,
        modifier =
            modifier
                .fillMaxWidth()
                .height(OutlineTextFieldHeightBasic)
                .then(
                    if (focusRequester != null) {
                        Modifier
                            .focusRequester(focusRequester)
                            .onGloballyPositioned {
                                if (enabled && requestFocusOnEnabled) {
                                    focusRequester.requestFocus()
                                }
                            }
                    } else {
                        Modifier
                    },
                ),
    )
}

/**
 * Single-line outlined text field with a trailing "auth code request" action.
 */
@Composable
fun OutlineTextField(
    textFieldState: TextFieldState,
    label: String,
    onAuthClick: () -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        state = textFieldState,
        lineLimits = TextFieldLineLimits.SingleLine,
        placeholder = { OutlineTextFieldPlaceholder(text = label) },
        shape = OutlineTextFieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        outputTransformation =
            if (keyboardType == KeyboardType.Password) {
                PasswordOutputTransformation
            } else {
                null
            },
        colors = outlineTextFieldBasicColorsSimple(),
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 25.dp),
            ) {
                Text(
                    text = stringResource(R.string.core_ui_request_verification_code),
                    modifier = Modifier.clickable { onAuthClick() },
                )
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .height(OutlineTextFieldHeightBasic),
    )
}

@Composable
fun OutlineTextField(
    textFieldState: TextFieldState,
    label: String,
    outputTransformation: OutputTransformation,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Phone,
) {
    OutlinedTextField(
        state = textFieldState,
        lineLimits = TextFieldLineLimits.SingleLine,
        placeholder = { OutlineTextFieldPlaceholder(text = label) },
        shape = OutlineTextFieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        outputTransformation = outputTransformation,
        colors = outlineTextFieldBasicColorsSimple(),
        modifier =
            modifier
                .fillMaxWidth()
                .height(OutlineTextFieldHeightBasic),
    )
}

/**
 * Single-line outlined text field that behaves like a file picker.
 */
@Composable
fun OutlineTextField(
    textFieldState: TextFieldState,
    label: String,
    onFileAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        state = textFieldState,
        lineLimits = TextFieldLineLimits.SingleLine,
        placeholder = { OutlineTextFieldPlaceholder(text = label) },
        shape = OutlineTextFieldShape,
        colors = outlineTextFieldBasicColorsSimple(),
        readOnly = true,
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 25.dp),
            ) {
                IconButton(onClick = { onFileAddClick() }) {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = stringResource(R.string.core_ui_content_description_add_file),
                        tint = AfternoteDesign.colors.gray9,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .height(OutlineTextFieldHeightBasic),
    )
}

// ============================================================================
// Labeled Variants (external label above field, filled style, 56.dp height)
// ============================================================================

/**
 * Labeled single-line text field with optional error state.
 *
 * **Use when**
 * - You need a label above the field (instead of a placeholder-only input).
 * - You want a filled look (border hidden) with a configurable container color.
 * - You optionally need error state with red border.
 *
 * @param modifier Modifier for the whole component.
 * @param label Label text shown above the field.
 * @param textFieldState Text field state holder.
 * @param placeholder Placeholder text shown when empty.
 * @param keyboardType Keyboard type; if [KeyboardType.Password], input is visually masked.
 * @param style Style configuration (container color, label spacing).
 * @param isError Whether to show the red error border.
 */
@Composable
fun OutlineTextField(
    label: String,
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.core_ui_text_field_placeholder),
    keyboardType: KeyboardType = KeyboardType.Text,
    style: LabeledTextFieldStyle = LabeledTextFieldStyle(),
    isError: Boolean = false,
) {
    val containerColor = style.containerColor ?: AfternoteDesign.colors.white
    val labelColorResolved = style.labelColor ?: AfternoteDesign.colors.gray9
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = style.labelSpacing),
    ) {
        Text(
            text = label,
            style =
                AfternoteDesign.typography.captionLargeR.copy(
                    fontSize = style.labelFontSize,
                    lineHeight = style.labelLineHeight,
                    fontWeight = style.labelFontWeight,
                    color = labelColorResolved,
                ),
        )

        OutlinedTextField(
            state = textFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            contentPadding =
                PaddingValues(
                    vertical = 16.dp,
                    horizontal = 24.dp,
                ),
            placeholder = { OutlineTextFieldPlaceholder(text = placeholder) },
            colors = outlineTextFieldFilledColors(containerColor),
            shape = OutlineTextFieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            outputTransformation =
                if (keyboardType == KeyboardType.Password) {
                    PasswordOutputTransformation
                } else {
                    null
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(OutlineTextFieldHeightLabeled)
                    .background(containerColor, OutlineTextFieldShape)
                    .then(
                        if (isError) {
                            Modifier.border(
                                width = 1.dp,
                                color = Red,
                                shape = OutlineTextFieldShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
            textStyle =
                AfternoteDesign.typography.textField.copy(
                    color = labelColorResolved,
                ),
        )
    }
}

// ============================================================================
// Multiline Variant (external label, larger container)
// ============================================================================

/**
 * Multiline message-style text field.
 *
 * @param modifier Modifier for the whole component.
 * @param label Label text shown above the field.
 * @param textFieldState Text field state holder.
 * @param placeholder Placeholder text shown when empty.
 */
@Composable
fun MultilineOutlineTextField(
    label: String,
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.core_ui_text_field_placeholder),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = label,
            style =
                AfternoteDesign.typography.textField.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = AfternoteDesign.colors.gray9,
        )

        OutlinedTextField(
            state = textFieldState,
            lineLimits = TextFieldLineLimits.MultiLine(),
            placeholder = { OutlineTextFieldPlaceholder(text = placeholder) },
            colors = outlineTextFieldFilledColors(AfternoteDesign.colors.white),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions.Default,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(OutlineTextFieldHeightMultiline)
                    .background(AfternoteDesign.colors.white, RoundedCornerShape(16.dp)),
            contentPadding = PaddingValues(all = 16.dp),
            textStyle =
                AfternoteDesign.typography.textField.copy(
                    color = AfternoteDesign.colors.gray9,
                ),
        )
    }
}

// ============================================================================
// Filled box variant (no label, multiline-style container)
// ============================================================================

/**
 * Multiline-style outlined text field with no label (e.g. "기타 직접 입력" box).
 *
 * @param modifier Modifier for the field.
 * @param textFieldState Text field state holder.
 * @param placeholder Placeholder text when empty.
 * @param containerColor Background and border fill color.
 * @param height Height of the field.
 * @param shape Shape of the container.
 */
@Composable
fun OutlineTextField(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.core_ui_text_field_placeholder),
    containerColor: Color? = null,
    height: Dp = OutlineTextFieldHeightMultiline,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
) {
    val bg = containerColor ?: AfternoteDesign.colors.gray1
    OutlinedTextField(
        state = textFieldState,
        lineLimits = TextFieldLineLimits.MultiLine(),
        placeholder = { OutlineTextFieldPlaceholder(text = placeholder) },
        colors = outlineTextFieldFilledColorsAll(bg),
        shape = shape,
        keyboardOptions = KeyboardOptions.Default,
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .background(bg, shape),
        contentPadding = PaddingValues(all = 16.dp),
        textStyle =
            AfternoteDesign.typography.textField.copy(
                color = AfternoteDesign.colors.gray9,
            ),
    )
}

// ============================================================================
// Previews
// ============================================================================

@Preview(showBackground = true, name = "기본 플레이스홀더")
@Composable
private fun OutlineTextFieldBasicPreview() {
    AfternoteTheme {
        OutlineTextField(
            textFieldState = rememberTextFieldState(),
            label = "시작",
            keyboardType = KeyboardType.Text,
            enabled = true,
        )
    }
}

@Preview(showBackground = true, name = "인증번호 받기 버튼")
@Composable
private fun OutlineTextFieldWithAuthPreview() {
    AfternoteTheme {
        OutlineTextField(
            textFieldState = rememberTextFieldState(),
            label = "전화번호",
            onAuthClick = {},
        )
    }
}

@Preview(showBackground = true, name = "파일 추가 버튼")
@Composable
private fun OutlineTextFieldWithFileAddPreview() {
    AfternoteTheme {
        OutlineTextField(
            textFieldState = rememberTextFieldState(),
            label = "파일 선택",
            onFileAddClick = {},
        )
    }
}

@Preview(showBackground = true, name = "라벨이 있는 버전")
@Composable
private fun OutlineTextFieldWithLabelPreview() {
    AfternoteTheme {
        OutlineTextField(
            label = "아이디",
            textFieldState = rememberTextFieldState(),
            placeholder = stringResource(R.string.core_ui_text_field_placeholder),
        )
    }
}

@Preview(showBackground = true, name = "에러 상태")
@Composable
private fun OutlineTextFieldWithErrorPreview() {
    AfternoteTheme {
        OutlineTextField(
            label = "비밀번호",
            textFieldState = rememberTextFieldState(),
            isError = true,
        )
    }
}

@Preview(showBackground = true, name = "비활성화 상태")
@Composable
private fun OutlineTextFieldDisabledPreview() {
    AfternoteTheme {
        OutlineTextField(
            textFieldState = rememberTextFieldState(),
            label = "비활성화된 필드",
            enabled = false,
        )
    }
}

@Preview(showBackground = true, name = "비밀번호 입력")
@Composable
private fun OutlineTextFieldPasswordPreview() {
    AfternoteTheme {
        OutlineTextField(
            textFieldState = rememberTextFieldState("password123"),
            label = "비밀번호",
            keyboardType = KeyboardType.Password,
            enabled = true,
        )
    }
}

@Preview(showBackground = true, name = "멀티라인 버전")
@Composable
private fun MultilineOutlineTextFieldPreview() {
    AfternoteTheme {
        MultilineOutlineTextField(
            label = "남기실 말씀",
            textFieldState = rememberTextFieldState(),
        )
    }
}
