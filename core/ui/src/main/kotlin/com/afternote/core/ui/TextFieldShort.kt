package com.afternote.core.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import android.graphics.Paint as AndroidPaint
import android.graphics.Rect as AndroidRect
import android.graphics.Typeface as AndroidTypeface
import android.view.KeyEvent as NativeKeyEvent

// ============================================================================
// 1. 공통 내부 구현체 (건드릴 필요 없음)
// ============================================================================

private const val PASSWORD_MASK_CHAR = "•"

/**
 * 비밀번호 마스킹 [OutputTransformation].
 *
 * 전체 범위를 `replace(0, length, ...)` 로 한 번에 치환하면 그 구간이 통째로 wedge 가 되어
 * 캐럿이 텍스트 중간에 들어가지 못하고 맨 앞/맨 뒤로만 스냅된다. 문자 단위 1:1 치환은
 * `OffsetMappingCalculator` 가 편집 연산으로 기록조차 하지 않아 오프셋 매핑이 identity 로 남는다.
 */
private val PasswordMaskOutputTransformation =
    OutputTransformation {
        for (i in 0 until length) {
            replace(i, i + 1, PASSWORD_MASK_CHAR)
        }
    }

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
    focusRequester: FocusRequester? = null,
    isError: Boolean = false,
) {
    BasicTextField(
        state = state,
        modifier =
            modifier
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
                ).fillMaxWidth(),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        onKeyboardAction = onImeAction?.let { action -> { action() } },
        inputTransformation = inputTransformation,
        outputTransformation =
            outputTransformation
                ?: if (keyboardType == KeyboardType.Password) {
                    PasswordMaskOutputTransformation
                } else {
                    null
                },
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        textStyle = AfternoteDesign.typography.textField.copy(color = AfternoteDesign.colors.gray9), // 👈 무조건 textField 스타일 고정!
        cursorBrush = SolidColor(AfternoteDesign.colors.black),
        decorator = { innerTextField ->
            AfternoteFieldContainer(
                modifier = Modifier.fillMaxWidth(),
                isError = isError,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
        val text: String,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
    ) : TextFieldType

    // Variant8: 하이픈 + 뒷자리 첫 숫자(실제 [BasicTextField]) + 마스킹 점.
    data class Variant8(
        val backState: TextFieldState,
        val placeholder: String = "0",
        val dotCount: Int = 6,
        val backFocusRequester: FocusRequester? = null,
        val frontFocusRequester: FocusRequester? = null,
    ) : TextFieldType
}

@Composable
fun AfternoteTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    type: TextFieldType = TextFieldType.Basic,
    placeholder: String = stringResource(R.string.core_ui_text_field_placeholder),
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    focusRequester: FocusRequester? = null,
    isError: Boolean = false,
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
        focusRequester = focusRequester,
        isError = isError,
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
                    { Variant8Suffix(type = type, onImeAction = onImeAction) }
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
        contentDescription = stringResource(R.string.core_ui_content_description_search),
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun Variant7Suffix(type: TextFieldType.Variant7) {
    Text(
        text = type.text,
        modifier =
            if (type.enabled) {
                Modifier
                    .clickable(role = Role.Button, onClick = type.onClick)
            } else {
                Modifier
            },
        style = AfternoteDesign.typography.captionLargeR,
        color = AfternoteDesign.colors.gray7,
    )
}

/** 숫자만 허용하고 최대 1글자(붙여넣기 포함). `maxLength`로 TalkBack 등 max 길이 시맨틱 반영. */
private val Variant8BackDigitInputTransformation =
    InputTransformation {
        val seq = asCharSequence()
        if (seq.any { !it.isDigit() } || seq.length > 1) {
            revertAllChanges()
        }
    }.maxLength(1)

/** 광학 중심을 재는 표본 글리프 — Variant8 뒷자리 입력은 숫자 한 자리다. */
private const val VARIANT8_DIGIT_SAMPLE = "0"

/**
 * 숫자 글리프의 **잉크 중심**이 라인박스 중심에서 얼마나 떨어져 있는지 (음수 = 위).
 *
 * [Row] 의 [Alignment.CenterVertically] 는 형제들의 *측정 높이* 중심을 맞춘다. 그런데
 * [BasicTextField] 의 높이는 라인박스(16/22sp)이고 그 중심은 descent 여백 때문에 글리프
 * 잉크 중심보다 아래에 있다. 그래서 라인박스 중심에 맞춘 하이픈·마스킹 점이 숫자보다 처져
 * 보인다 — 실측 1.14dp (#1496).
 *
 * 고정 오프셋을 박아 넣으면 폰트·사이즈가 바뀔 때 조용히 어긋나므로, 실제 폰트에서 매번 잰다.
 * 잉크 상·하단은 [AndroidPaint.getTextBounds] 가 주는 글리프 경계이고, baseline 과 라인박스
 * 높이는 같은 스타일로 측정한 [androidx.compose.ui.text.TextMeasurer] 결과를 쓴다.
 */
@Composable
private fun rememberDigitOpticalCenterShift(style: TextStyle): Dp {
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val textMeasurer = rememberTextMeasurer()
    return remember(density, fontFamilyResolver, textMeasurer, style) {
        val layout = textMeasurer.measure(text = VARIANT8_DIGIT_SAMPLE, style = style)
        val typeface =
            fontFamilyResolver
                .resolve(
                    fontFamily = style.fontFamily,
                    fontWeight = style.fontWeight ?: FontWeight.Normal,
                    fontStyle = style.fontStyle ?: FontStyle.Normal,
                    fontSynthesis = style.fontSynthesis ?: FontSynthesis.All,
                ).value as AndroidTypeface
        val paint =
            AndroidPaint().apply {
                this.typeface = typeface
                textSize = with(density) { style.fontSize.toPx() }
            }
        val ink =
            AndroidRect().also {
                paint.getTextBounds(VARIANT8_DIGIT_SAMPLE, 0, VARIANT8_DIGIT_SAMPLE.length, it)
            }
        // getTextBounds 는 baseline 기준이라 top 이 음수다. 둘의 중점이 잉크 중심.
        val inkCenter = layout.firstBaseline + (ink.top + ink.bottom) / 2f
        val lineBoxCenter = layout.size.height / 2f
        with(density) { (inkCenter - lineBoxCenter).toDp() }
    }
}

@Composable
private fun Variant8Suffix(
    type: TextFieldType.Variant8,
    onImeAction: (() -> Unit)?,
) {
    val backInputContentDescription =
        stringResource(R.string.core_ui_content_description_resident_number_back_input)
    val textStyle = AfternoteDesign.typography.textField
    // 하이픈·점은 텍스트 라인박스가 아니라 숫자 글리프의 광학 중심에 맞춘다 (#1496).
    val opticalCenterShift = rememberDigitOpticalCenterShift(textStyle)

    Row(
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = backInputContentDescription
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 1. 고정된 하이픈 — 폰트 여백 없이 정확히 14x1.75 크기로 고정 (시안 Vector 58 strokeWeight)
        Box(
            modifier =
                Modifier
                    .offset(y = opticalCenterShift)
                    .width(14.dp)
                    .height(1.75.dp)
                    .background(
                        color = AfternoteDesign.colors.gray9,
                    ),
        )

        // 2. 뒷자리 첫 숫자 (실제 입력)
        BasicTextField(
            state = type.backState,
            modifier =
                Modifier
                    .widthIn(min = 12.dp)
                    .width(IntrinsicSize.Min)
                    .then(
                        if (type.backFocusRequester != null) {
                            Modifier.focusRequester(type.backFocusRequester)
                        } else {
                            Modifier
                        },
                    ).onPreviewKeyEvent { event ->
                        val isBackspace =
                            event.key == Key.Backspace ||
                                event.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DEL
                        if (isBackspace &&
                            event.type == KeyEventType.KeyDown &&
                            type.backState.text.isEmpty()
                        ) {
                            type.frontFocusRequester?.requestFocus()
                            true
                        } else {
                            false
                        }
                    },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
            onKeyboardAction = { onImeAction?.invoke() },
            inputTransformation = Variant8BackDigitInputTransformation,
            textStyle = textStyle.copy(color = AfternoteDesign.colors.black),
            cursorBrush = SolidColor(AfternoteDesign.colors.black),
            interactionSource = remember { MutableInteractionSource() },
            decorator = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (type.backState.text.isEmpty()) {
                        Text(
                            text = type.placeholder,
                            style = AfternoteDesign.typography.textField,
                            color = AfternoteDesign.colors.gray4,
                        )
                    }
                    innerTextField()
                }
            },
        )

        // 3. 고정된 마스킹 점
        Variant8MaskDots(
            dotCount = type.dotCount,
            modifier = Modifier.offset(y = opticalCenterShift),
        )
    }
}

@Composable
private fun Variant8MaskDots(
    dotCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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

            val variant8Back = rememberTextFieldState()
            AfternoteTextField(
                state = rememberTextFieldState(),
                type = TextFieldType.Variant8(backState = variant8Back),
                placeholder = "Variant 8",
            )
        }
    }
}
