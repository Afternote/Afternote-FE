package com.afternote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

private val TextFieldShape = RoundedCornerShape(8.dp)
private val TextFieldContentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp)
private val SuffixGap = 4.dp
private val TrailingContentGap = 8.dp
private val BorderWidth = 1.dp

private const val PASSWORD_MASK_CHAR = '\u2022'

/**
 * 비밀번호 마스킹용 OutputTransformation.
 * [TextFieldShort]에서 keyboardType이 [KeyboardType.Password]일 때 자동 적용됩니다.
 */
val PasswordMaskTransformation =
    OutputTransformation {
        val originalLength = length
        replace(0, originalLength, PASSWORD_MASK_CHAR.toString().repeat(originalLength))
    }

/**
 * [AfternoteTextField] 내부 구현체.
 *
 * Foundation [BasicTextField] + decorator 기반으로 Material3 강제 패딩 없이
 * 디자인 시안을 픽셀 단위로 제어합니다. 외부에서는 [AfternoteTextField]를 사용하고,
 * 여기 노출된 슬롯들은 래퍼가 타입별로 채웁니다.
 *
 * @param suffix 텍스트 바로 옆에 붙는 요소 (e.g. 단위, 도트). 텍스트 길이에 따라 위치가 움직입니다.
 * @param trailingContent 항상 필드 맨 우측 끝에 고정되는 요소 (e.g. 검색 아이콘).
 * @param showOutline false면 테두리를 그리지 않습니다.
 * @param interactionSource 포커스 감지 등 외부에서 구독할 때 전달. null이면 내부에서 생성.
 * @param onFocusChanged 포커스 진입/이탈 알림.
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
    containerColor: Color? = null,
    focusRequester: FocusRequester? = null,
    /** [imeAction]으로 지정한 IME 액션(예: Done)이 눌릴 때. */
    onImeAction: (() -> Unit)? = null,
    showOutline: Boolean = true,
    textStyle: TextStyle? = null,
    interactionSource: MutableInteractionSource? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by resolvedInteractionSource.collectIsFocusedAsState()
    val bgColor = containerColor ?: AfternoteDesign.colors.white

    val borderColor =
        when {
            !showOutline -> Color.Transparent
            isFocused -> AfternoteDesign.colors.black
            containerColor != null -> Color.Transparent
            else -> AfternoteDesign.colors.gray2
        }

    val actualOutputTransformation =
        outputTransformation
            ?: if (keyboardType == KeyboardType.Password) PasswordMaskTransformation else null

    val resolvedTextStyle =
        textStyle
            ?: AfternoteDesign.typography.textField.copy(
                color = AfternoteDesign.colors.gray9,
            )

    BasicTextField(
        state = state,
        modifier =
            modifier
                .fillMaxWidth()
                .background(bgColor, TextFieldShape)
                .then(
                    if (showOutline) {
                        Modifier.border(BorderWidth, borderColor, TextFieldShape)
                    } else {
                        Modifier
                    },
                ).then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
                ).then(
                    if (onFocusChanged != null) {
                        Modifier.onFocusChanged { onFocusChanged(it.isFocused) }
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
        outputTransformation = actualOutputTransformation,
        interactionSource = resolvedInteractionSource,
        textStyle = resolvedTextStyle,
        cursorBrush = SolidColor(AfternoteDesign.colors.black),
        decorator = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(TextFieldContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 텍스트 + suffix를 하나로 묶어 왼쪽 정렬 유지
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f, fill = false),
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
                        Spacer(modifier = Modifier.width(SuffixGap))
                        suffix()
                    }
                }

                // trailingContent는 항상 필드 맨 우측 끝에 고정
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(TrailingContentGap))
                    trailingContent()
                }
            }
        },
    )
}

// ============================================================================
// 1. 피그마 구조와 매핑되는 Enum 정의
// ============================================================================

/**
 * 피그마 텍스트 필드 Variant와 1:1로 매칭되는 타입.
 *
 * 상태(포커스·입력 여부)는 런타임에 자동으로 전환되므로 Variant로 분리하지 않습니다.
 * 구조(생김새)가 다른 것만 열거합니다.
 */
enum class TextFieldType {
    /** Figma: nonfield, writing, write, field (기본 형태) */
    Basic,

    /** Figma: nonsearch, search (우측 돋보기 아이콘) */
    Search,

    /** Figma: Variant7 (우측 텍스트 접미사) */
    Variant7,

    /** Figma: Variant8 (복합 기호 접미사: 대시·T·도트 6개) */
    Variant8,

    /** Figma: Variant9 (회색 배경 URL 입력 필드) */
    Variant9,
}

// ============================================================================
// 2. 피그마 Variant 이름으로 호출하는 Wrapper 컴포넌트
// ============================================================================

/**
 * Afternote 디자인 시스템 공용 텍스트 필드.
 *
 * 디자이너 시안에서 본 피그마 Variant 이름을 [type]에 그대로 넘기면 해당 Variant의
 * 배경색·접미사·우측 아이콘이 자동 세팅됩니다.
 *
 * 기본 사양을 벗어나는 멀티라인·인라인 편집 등 변형이 필요한 곳은
 * [androidx.compose.foundation.text.BasicTextField]로 직접 구현해 주세요.
 */
@Composable
fun AfternoteTextField(
    state: TextFieldState,
    type: TextFieldType,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    focusRequester: FocusRequester? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val containerColor =
        if (type == TextFieldType.Variant9) AfternoteDesign.colors.gray1 else null

    val effectiveTrailingContent: @Composable (() -> Unit)? =
        trailingContent
            ?: if (type == TextFieldType.Search) {
                {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색",
                        tint = AfternoteDesign.colors.gray9,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                null
            }

    val suffix: @Composable (() -> Unit)? =
        when (type) {
            TextFieldType.Variant7 -> {
                {
                    Text(
                        text = "Text Field",
                        style = AfternoteDesign.typography.textField,
                        color = AfternoteDesign.colors.gray4,
                    )
                }
            }

            TextFieldType.Variant8 -> {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "—",
                            style = AfternoteDesign.typography.textField,
                            color = AfternoteDesign.colors.black,
                        )
                        Text(
                            text = "T",
                            style = AfternoteDesign.typography.textField,
                            color = AfternoteDesign.colors.gray4,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(6) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(8.dp)
                                            .background(
                                                AfternoteDesign.colors.black,
                                                RoundedCornerShape(50),
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
        focusRequester = focusRequester,
        containerColor = containerColor,
        trailingContent = effectiveTrailingContent,
        suffix = suffix,
    )
}

// ============================================================================
// 3. 피그마 9종 카탈로그 프리뷰 (타입 이름으로만 호출)
// ============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFC0C0C0, name = "AfternoteTextField 피그마 9종")
@Composable
private fun AfternoteTextFieldFigmaPreview() {
    AfternoteTheme {
        val focusReqWriting = remember { FocusRequester() }
        val focusReqWrite = remember { FocusRequester() }

        // 프리뷰 환경에서 '포커스된 상태'를 강제로 보여주기 위함
        LaunchedEffect(Unit) {
            focusReqWriting.requestFocus()
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. nonfield
            AfternoteTextField(
                type = TextFieldType.Basic,
                state = rememberTextFieldState(),
                placeholder = "Text Field",
            )

            // 2. writing (포커스 O)
            AfternoteTextField(
                type = TextFieldType.Basic,
                state = rememberTextFieldState(),
                focusRequester = focusReqWriting,
            )

            // 3. write (글자 O, 포커스 O)
            AfternoteTextField(
                type = TextFieldType.Basic,
                state = rememberTextFieldState("Text Field"),
                focusRequester = focusReqWrite,
            )

            // 4. field (글자 O)
            AfternoteTextField(
                type = TextFieldType.Basic,
                state = rememberTextFieldState("Text Field"),
            )

            // 5. nonsearch
            AfternoteTextField(
                type = TextFieldType.Search,
                state = rememberTextFieldState(),
                placeholder = "Text Field",
            )

            // 6. search
            AfternoteTextField(
                type = TextFieldType.Search,
                state = rememberTextFieldState("Text Field"),
            )

            // 7. Variant7
            AfternoteTextField(
                type = TextFieldType.Variant7,
                state = rememberTextFieldState("Text Field"),
            )

            // 8. Variant8
            AfternoteTextField(
                type = TextFieldType.Variant8,
                state = rememberTextFieldState("Text Field"),
            )

            // 9. Variant9
            AfternoteTextField(
                type = TextFieldType.Variant9,
                state = rememberTextFieldState(),
                placeholder = "URL을 입력하세요.",
            )
        }
    }
}
