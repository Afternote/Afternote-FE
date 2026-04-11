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
import androidx.compose.foundation.layout.defaultMinSize
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
private val TextFieldMinHeight = 56.dp
private val TextFieldContentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
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
 * Afternote 디자인 시스템 공용 단일 라인 텍스트 필드.
 *
 * Foundation [BasicTextField] + decorator 기반으로 Material3 강제 패딩 없이
 * 디자인 시안을 픽셀 단위로 제어합니다. 피그마 기본 사양(56dp 높이, 16dp 패딩,
 * 8dp radius) 하나에만 대응합니다. 멀티라인·인라인·라벨 있는 필드 등
 * 변형 사양이 필요한 호출부는 [BasicTextField]로 직접 구현해 주세요.
 *
 * @param suffix 텍스트 바로 옆에 붙는 요소 (e.g. 단위, 도트). 텍스트 길이에 따라 위치가 움직입니다.
 * @param trailingContent 항상 필드 맨 우측 끝에 고정되는 요소 (e.g. 검색 아이콘).
 * @param showOutline false면 테두리를 그리지 않습니다.
 * @param textStyle null이면 [AfternoteDesign.typography.textField] 기준.
 * @param interactionSource 포커스 감지 등 외부에서 구독할 때 전달. null이면 내부에서 생성.
 * @param onFocusChanged 포커스 진입/이탈 알림.
 */
@Composable
fun TextFieldShort(
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
                .defaultMinSize(minHeight = TextFieldMinHeight)
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
// Previews — 피그마 디자인 시스템의 텍스트 필드 상태
// ============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFC0C0C0, name = "전체 상태 카탈로그")
@Composable
private fun TextFieldShortCatalogPreview() {
    AfternoteTheme {
        val previewFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            previewFocusRequester.requestFocus()
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. 기본 (Placeholder만 표시)
            TextFieldShort(
                state = rememberTextFieldState(),
                placeholder = "Text Field",
            )

            // 2. 활성 (포커스 테두리 + 커서)
            TextFieldShort(
                state = rememberTextFieldState(),
                focusRequester = previewFocusRequester,
            )

            // 3. 입력 중 (텍스트)
            TextFieldShort(
                state = rememberTextFieldState("Text Field"),
            )

            // 4. 검색 아이콘 (trailingContent → 우측 끝 고정)
            TextFieldShort(
                state = rememberTextFieldState(),
                placeholder = "Text Field",
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AfternoteDesign.colors.gray9,
                        modifier = Modifier.size(24.dp),
                    )
                },
            )

            // 5. 우측 힌트 텍스트 (suffix → 텍스트 바로 옆)
            TextFieldShort(
                state = rememberTextFieldState(),
                placeholder = "Text Field",
                suffix = {
                    Text(
                        text = "Text Field",
                        style = AfternoteDesign.typography.textField,
                        color = AfternoteDesign.colors.gray4,
                    )
                },
            )

            // 6. URL 입력 필드
            TextFieldShort(
                state = rememberTextFieldState(),
                placeholder = "URL을 입력하세요.",
                containerColor = AfternoteDesign.colors.gray1,
            )
        }
    }
}
