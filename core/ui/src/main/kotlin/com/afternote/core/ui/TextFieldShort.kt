package com.afternote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

// ============================================================================
// 1. 공통 내부 구현체 (건드릴 필요 없음)
// ============================================================================

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
    val textFieldShape = RoundedCornerShape(8.dp)
    BasicTextField(
        state = state,
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.white, textFieldShape)
                .border(1.dp, AfternoteDesign.colors.gray2, textFieldShape),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        onKeyboardAction = onImeAction?.let { action -> { action() } },
        inputTransformation = inputTransformation,
        outputTransformation =
            outputTransformation
                ?: if (keyboardType == KeyboardType.Password) {
                    OutputTransformation {
                        replace(0, length, '\u2022'.toString().repeat(length))
                    }
                } else {
                    null
                },
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        textStyle = AfternoteDesign.typography.textField.copy(color = AfternoteDesign.colors.gray9), // 👈 무조건 textField 스타일 고정!
        cursorBrush = SolidColor(AfternoteDesign.colors.black),
        decorator = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f, fill = false)
                                .width(IntrinsicSize.Max),
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

                    if (suffix != null) {
                        Spacer(Modifier.width(8.dp))
                        suffix()
                    }
                }

                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingContent()
                }
            }
        },
    )
}

// ============================================================================
// 2. 타입 정의 + 메인 Public API
// ============================================================================

sealed interface TextFieldType {
    data object Basic : TextFieldType

    data object Search : TextFieldType

    // Variant7을 쓸 때만 텍스트와 클릭 이벤트를 '필수'로 강제합니다.
    data class Variant7(
        val text: String = "인증번호 받기",
        val onClick: () -> Unit,
    ) : TextFieldType

    // Variant8: 하이픈 + 첫 자리 숫자(플레이스홀더/입력값) + 마스킹 점들을 독립 요소로 조합.
    data class Variant8(
        val firstDigit: String = "", // 사용자가 입력한 숫자 (비어있으면 플레이스홀더 노출)
        val placeholder: String = "0", // 0은 순수 플레이스홀더 역할
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
    TextFieldShort(
        state = state,
        modifier = modifier,
        placeholder = placeholder,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeAction = onImeAction,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        trailingContent =
            when (type) {
                TextFieldType.Search -> {
                    { SearchIcon() }
                }

                else -> {
                    null
                }
            },
        suffix =
            when (type) {
                is TextFieldType.Variant7 -> {
                    { Variant7Suffix(type) }
                }

                is TextFieldType.Variant8 -> {
                    { Variant8Suffix(type) }
                }

                else -> {
                    null
                }
            },
    )
}

// ============================================================================
// 3. 컴포넌트 파편화 (UI 덩어리들을 밖으로 빼냄)
// ============================================================================

@Composable
private fun SearchIcon() {
    Icon(
        painter = painterResource(R.drawable.core_ui_ic_tabler_search),
        contentDescription = "검색",
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun Variant7Suffix(type: TextFieldType.Variant7) {
    Text(
        text = type.text,
        modifier = Modifier.clickable(onClick = type.onClick),
        style = AfternoteDesign.typography.captionLargeR,
        color = AfternoteDesign.colors.gray7,
    )
}

@Composable
private fun Variant8Suffix(type: TextFieldType.Variant8) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 1. 고정된 하이픈 — 폰트 여백 없이 정확히 14x2 크기로 고정
        Box(
            modifier =
                Modifier
                    .width(14.dp)
                    .height(1.dp)
                    .background(
                        color = AfternoteDesign.colors.gray9,
                    ),
        )

        // 2. 첫 자리 숫자 (플레이스홀더 vs 실제 입력값)
        val isPlaceholder = type.firstDigit.isEmpty()
        Text(
            text = if (isPlaceholder) type.placeholder else type.firstDigit,
            style = AfternoteDesign.typography.textField,
            color =
                if (isPlaceholder) {
                    AfternoteDesign.colors.gray4
                } else {
                    AfternoteDesign.colors.black
                },
        )

        // 3. 고정된 마스킹 점
        Variant8MaskDots(dotCount = type.dotCount)
    }
}

@Composable
private fun Variant8MaskDots(dotCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(dotCount) {
            Box(
                modifier =
                    Modifier
                        .size(14.dp)
                        .background(
                            color = AfternoteDesign.colors.black,
                            shape = CircleShape,
                        ),
            )
        }
    }
}

// ============================================================================
// 4. 피그마 9종 카탈로그 프리뷰 (타입 이름으로만 호출)
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
