package com.afternote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

private val TextFieldShape = RoundedCornerShape(8.dp)

// private val TextFieldContentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp)
private val TextFieldContentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp)
private val SuffixGap = 8.dp
private val TrailingContentGap = 8.dp
private val BorderWidth = 1.dp

private const val PASSWORD_MASK_CHAR = '\u2022'

val PasswordMaskTransformation =
    OutputTransformation {
        val originalLength = length
        replace(0, originalLength, PASSWORD_MASK_CHAR.toString().repeat(originalLength))
    }

/**
 * [AfternoteTextField] 내부 구현체.
 * 철저히 하드코딩된 스타일(textField)을 사용하며, 파라미터를 최소화했습니다.
 */
@Composable
private fun TextFieldShort(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    suffix: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    onImeAction: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    val borderColor = AfternoteDesign.colors.gray2

    val actualOutputTransformation =
        outputTransformation
            ?: if (keyboardType == KeyboardType.Password) PasswordMaskTransformation else null

    BasicTextField(
        state = state,
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.white, TextFieldShape)
                .border(BorderWidth, borderColor, TextFieldShape),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        onKeyboardAction = onImeAction?.let { action -> { action() } },
        inputTransformation = inputTransformation,
        outputTransformation = actualOutputTransformation,
        interactionSource = resolvedInteractionSource,
        textStyle = AfternoteDesign.typography.textField.copy(color = AfternoteDesign.colors.gray9), // 👈 무조건 textField 스타일 고정!
        cursorBrush = SolidColor(AfternoteDesign.colors.black),
        decorator = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(TextFieldContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 이렇게 해야 피그마랑 똑같아지긴 하는데 무슨 의도인지 모르겠어서 주석 처리함
//                    Box(
//                        modifier =
//                            Modifier
//                                .weight(1f, fill = false)
//                                .width(IntrinsicSize.Max),
//                        contentAlignment = Alignment.CenterStart,
//                    ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.text.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = AfternoteDesign.typography.textField, // 👈 무조건 textField 고정
                                color = AfternoteDesign.colors.gray4,
                            )
                        }
                        innerTextField()
                    }

                    if (suffix != null) {
                        Spacer(modifier = Modifier.width(SuffixGap))
                        suffix()
                    }
                }

                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(TrailingContentGap))
                    trailingContent()
                }
            }
        },
    )
}

sealed interface TextFieldType {
    data object Basic : TextFieldType

    data object Search : TextFieldType

    // Variant7을 쓸 때만 텍스트와 클릭 이벤트를 '필수'로 강제합니다.
    data class Variant7(
        val text: String = "인증번호 받기",
        val onClick: () -> Unit,
    ) : TextFieldType

    // Variant8: 남은 시간 텍스트와 점의 개수를 외부에서 주입.
    data class Variant8(
        val timeText: String = "— 0",
        val dotCount: Int = 6,
    ) : TextFieldType
}

@Composable
fun AfternoteTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    type: TextFieldType = TextFieldType.Basic,
    placeholder: String = "Text Field",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
) {
    // 디자인 시스템의 일관성을 위해, trailingContent 는 외부 주입 없이
    // 온전히 [TextFieldType] 에 따라 내부에서 결정됩니다.
    val trailingContent: @Composable (() -> Unit)? =
        when (type) {
            TextFieldType.Search -> {
                {
                    Icon(
                        painter = painterResource(R.drawable.core_ui_ic_tabler_search),
                        contentDescription = "검색",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            else -> {
                null
            }
        }

    val suffix: @Composable (() -> Unit)? =
        when (type) {
            is TextFieldType.Variant7 -> {
                {
                    Text(
                        text = type.text,
                        modifier =
                            Modifier.clickable(onClick = type.onClick),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray7,
                    )
                }
            }

            is TextFieldType.Variant8 -> {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        Text(
                            text = type.timeText,
                            style = AfternoteDesign.typography.textField,
                            color = AfternoteDesign.colors.black,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(type.dotCount) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(14.dp)
                                            .background(
                                                AfternoteDesign.colors.black,
                                                shape = CircleShape,
                                            ),
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                null
            }
        }

    TextFieldShort(
        state = state,
        modifier = modifier,
        placeholder = placeholder,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeAction = onImeAction,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        trailingContent = trailingContent,
        suffix = suffix,
    )
}

// ============================================================================
// 3. 피그마 9종 카탈로그 프리뷰 (타입 이름으로만 호출)
// ============================================================================

@Preview(
    showBackground = true,
    backgroundColor = 0xFFC0C0C0,
    name = "AfternoteTextField 피그마 9종 (ALL EMPTY)",
)
@Composable
private fun AfternoteTextFieldFigmaPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AfternoteTextField(
                state = rememberTextFieldState(),
                placeholder = "nonfield/writing/write/field",
            )

            AfternoteTextField(
                state = rememberTextFieldState(),
                type = TextFieldType.Search,
                placeholder = "nonsearch/search",
            )

            AfternoteTextField(
                state = rememberTextFieldState(),
                type =
                    TextFieldType.Variant7(
                        text = "인증번호 받기",
                        onClick = { },
                    ),
                placeholder = "Variant 7",
            )

            AfternoteTextField(
                state = rememberTextFieldState(),
                type = TextFieldType.Variant8(),
                placeholder = "Variant 8",
            )
        }
    }
}
