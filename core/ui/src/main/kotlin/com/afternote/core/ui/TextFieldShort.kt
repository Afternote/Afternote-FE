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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Red

private val TextFieldShape = RoundedCornerShape(8.dp)
private val TextFieldMinHeight = 56.dp

private const val PASSWORD_MASK_CHAR = '\u2022'

/**
 * [TextFieldShort] 테두리 등 상태별 색상. 호출부에서 덮어써 이벤트·변형 UI에 맞출 수 있습니다.
 */
data class AfternoteTextFieldColors(
    val defaultBorderColor: Color,
    val focusedBorderColor: Color,
    val errorBorderColor: Color,
    val transparentBorderColor: Color = Color.Transparent,
) {
    fun borderColor(
        showOutline: Boolean,
        isError: Boolean,
        isFocused: Boolean,
        hasContainerColor: Boolean,
    ): Color =
        when {
            !showOutline -> transparentBorderColor
            isError -> errorBorderColor
            isFocused -> focusedBorderColor
            hasContainerColor -> transparentBorderColor
            else -> defaultBorderColor
        }
}

/** [TextFieldShort] 기본값 팩토리 (Afternote 디자인 토큰). */
object AfternoteTextFieldDefaults {
    @Composable
    fun colors(
        defaultBorderColor: Color = AfternoteDesign.colors.gray2,
        focusedBorderColor: Color = AfternoteDesign.colors.black,
        errorBorderColor: Color = Red,
        transparentBorderColor: Color = Color.Transparent,
    ): AfternoteTextFieldColors =
        AfternoteTextFieldColors(
            defaultBorderColor = defaultBorderColor,
            focusedBorderColor = focusedBorderColor,
            errorBorderColor = errorBorderColor,
            transparentBorderColor = transparentBorderColor,
        )
}

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
 * Afternote 디자인 시스템 공용 텍스트 필드.
 *
 * Foundation [BasicTextField] + decorator 기반으로 Material3 강제 패딩 없이
 * 디자인 시안을 픽셀 단위로 제어합니다.
 * 싱글라인·멀티라인·패스워드 등 모든 용도를 이 하나의 컴포넌트로 커버합니다.
 *
 * @param suffix 텍스트 바로 옆에 붙는 요소 (e.g. 단위, 도트). 텍스트 길이에 따라 위치가 움직입니다.
 * @param trailingContent 항상 필드 맨 우측 끝에 고정되는 요소 (e.g. 검색 아이콘).
 * @param showOutline false면 테두리를 그리지 않습니다 (복합 입력·인라인 편집 등).
 * @param textStyle null이면 [AfternoteDesign.typography.textField] 기준.
 * @param placeholderTextStyle placeholder 전용 스타일. null이면 [AfternoteDesign.typography.textField].
 * @param interactionSource 포커스 감지 등 외부에서 구독할 때 전달. null이면 내부에서 생성.
 * @param onFocusChanged 포커스 진입/이탈 알림.
 * @param expandTextAreaToAvailableWidth `true`이면 텍스트 박스가 가로 남는 공간을 채우고 [Alignment.Center]로 둡니다
 * (주민번호 앞자리처럼 `textStyle`에 `TextAlign.Center`를 쓰는 경우).
 * 넓은 슬롯에 왼쪽 정렬만 필요하면 `false`로 두어 [Alignment.CenterStart]가 쓰이게 합니다.
 *
 * **주의:** [suffix]가 null이 아니면 위 확장 동작은 적용되지 않으며, 텍스트 박스는 내용 너비에 맞게 두고
 * 접미가 텍스트 바로 옆에 붙습니다 (`hasSuffix`로 확장이 꺼짐).
 * @param minWidth [BasicTextField] 최소 너비 (e.g. 한 자리 숫자 칸).
 * @param colors 테두리 등 상태 색상. 기본은 디자인 시스템; 필요 시 [AfternoteTextFieldDefaults.colors]로 일부만 오버라이드.
 */
@Composable
fun TextFieldShort(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    containerColor: Color? = null,
    minHeight: Dp = TextFieldMinHeight,
    minWidth: Dp? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    shape: Shape = TextFieldShape,
    labelSpacing: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    focusRequester: FocusRequester? = null,
    /** [imeAction]으로 지정한 IME 액션(예: Done)이 눌릴 때. */
    onImeAction: (() -> Unit)? = null,
    showOutline: Boolean = true,
    textStyle: TextStyle? = null,
    placeholderTextStyle: TextStyle? = null,
    interactionSource: MutableInteractionSource? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    expandTextAreaToAvailableWidth: Boolean = false,
    colors: AfternoteTextFieldColors = AfternoteTextFieldDefaults.colors(),
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by resolvedInteractionSource.collectIsFocusedAsState()
    val bgColor = containerColor ?: AfternoteDesign.colors.white

    val borderColor =
        colors.borderColor(
            showOutline = showOutline,
            isError = isError,
            isFocused = isFocused,
            hasContainerColor = containerColor != null,
        )

    val actualOutputTransformation =
        outputTransformation
            ?: if (keyboardType == KeyboardType.Password) PasswordMaskTransformation else null

    val isMultiline = lineLimits !is TextFieldLineLimits.SingleLine

    val resolvedTextStyle =
        textStyle
            ?: AfternoteDesign.typography.textField.copy(
                color = AfternoteDesign.colors.gray9,
            )
    val resolvedPlaceholderStyle = placeholderTextStyle ?: AfternoteDesign.typography.textField

    // expandTextAreaToAvailableWidth == true → 슬롯을 채운 뒤 수평·수직 중앙 (PIN/주민번호 앞자리 + TextAlign.Center)
    val textBoxAlignment =
        when {
            isMultiline -> Alignment.TopStart
            expandTextAreaToAvailableWidth -> Alignment.Center
            else -> Alignment.CenterStart
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
                    .defaultMinSize(
                        minWidth = minWidth ?: Dp.Unspecified,
                        minHeight = minHeight,
                    ).background(bgColor, shape)
                    .then(
                        if (showOutline) {
                            Modifier.border(1.dp, borderColor, shape)
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
            lineLimits = lineLimits,
            readOnly = readOnly,
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
            enabled = enabled,
            textStyle = resolvedTextStyle,
            cursorBrush = SolidColor(AfternoteDesign.colors.black),
            decorator = { innerTextField ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(contentPadding),
                    verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically,
                ) {
                    if (prefix != null) {
                        prefix()
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // 텍스트 + suffix를 하나로 묶어 왼쪽 정렬 유지
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically,
                    ) {
                        TextFieldTextBox(
                            expandTextAreaToAvailableWidth = expandTextAreaToAvailableWidth,
                            hasSuffix = suffix != null,
                            textBoxAlignment = textBoxAlignment,
                            placeholder = placeholder,
                            resolvedPlaceholderStyle = resolvedPlaceholderStyle,
                            state = state,
                            innerTextField = innerTextField,
                        )

                        if (suffix != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            suffix()
                        }
                    }

                    // trailingContent는 항상 필드 맨 우측 끝에 고정
                    if (trailingContent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        trailingContent()
                    }
                }
            },
        )
    }
}

@Composable
private fun RowScope.TextFieldTextBox(
    expandTextAreaToAvailableWidth: Boolean,
    hasSuffix: Boolean,
    textBoxAlignment: Alignment,
    placeholder: String?,
    resolvedPlaceholderStyle: TextStyle,
    state: TextFieldState,
    innerTextField: @Composable () -> Unit,
) {
    // suffix가 있으면 weight(1f)로 박스가 가로를 전부 먹어 접미가 우측 끝으로 밀리므로 확장 비활성화
    val shouldExpand = expandTextAreaToAvailableWidth && !hasSuffix
    Box(
        modifier =
            if (shouldExpand) {
                Modifier.weight(1f)
            } else {
                Modifier.weight(1f, fill = false)
            },
        contentAlignment = textBoxAlignment,
    ) {
        if (state.text.isEmpty() && placeholder != null) {
            Text(
                text = placeholder,
                style = resolvedPlaceholderStyle,
                color = AfternoteDesign.colors.gray4,
            )
        }
        innerTextField()
    }
}

// ============================================================================
// Previews — 피그마 디자인 시스템의 모든 텍스트 필드 상태
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

            // 4. 입력 완료
            TextFieldShort(
                state = rememberTextFieldState("Text Field"),
            )

            // 5. 검색 아이콘 (trailingContent → 우측 끝 고정)
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

            // 6. 검색 아이콘 (입력됨)
            TextFieldShort(
                state = rememberTextFieldState("Text Field"),
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AfternoteDesign.colors.gray9,
                        modifier = Modifier.size(24.dp),
                    )
                },
            )

            // 7. 우측 힌트 텍스트 (suffix → 텍스트 바로 옆)
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

            // 8. 복합 suffix (대시 + T + 도트 → 텍스트 바로 옆)
            TextFieldShort(
                state = rememberTextFieldState(),
                placeholder = "Text Field",
                suffix = {
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
                },
            )

            // 9. URL 입력 필드
            TextFieldShort(
                state = rememberTextFieldState(),
                placeholder = "URL을 입력하세요.",
                containerColor = AfternoteDesign.colors.gray1,
            )
        }
    }
}

@Preview(showBackground = true, name = "라벨 + 에러")
@Composable
private fun TextFieldShortLabeledPreview() {
    AfternoteTheme {
        val previewFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            previewFocusRequester.requestFocus()
        }
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextFieldShort(
                state = rememberTextFieldState(),
                label = "아이디",
                placeholder = "Text Field",
                focusRequester = previewFocusRequester,
            )
            TextFieldShort(
                state = rememberTextFieldState(),
                label = "비밀번호",
                placeholder = "Text Field",
                keyboardType = KeyboardType.Password,
                isError = true,
            )
        }
    }
}

private enum class TextFieldShortPlaygroundSlot {
    None,
    Prefix,
    Suffix,
    Trailing,
}

@Composable
private fun TextFieldShortPlaygroundLabeledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Composable
private fun TextFieldShortPlaygroundSlotRow(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = title,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

/**
 * 개발용: [TextFieldShort] 파라미터를 프리뷰에서 조작해 레이아웃·테두리 동작을 확인합니다.
 */
@Preview(
    name = "인터랙티브 플레이그라운드",
    showBackground = true,
    heightDp = 900,
    widthDp = 392,
)
@Composable
private fun TextFieldShortInteractivePlaygroundPreview() {
    AfternoteTheme {
        val scroll = rememberScrollState()
        var showLabel by remember { mutableStateOf(true) }
        var placeholderText by remember { mutableStateOf("Text Field") }
        val fieldState = rememberTextFieldState()
        var isError by remember { mutableStateOf(false) }
        var expandWidth by remember { mutableStateOf(false) }
        var showOutline by remember { mutableStateOf(true) }
        var slot by remember { mutableStateOf(TextFieldShortPlaygroundSlot.None) }

        val hasSuffix = slot == TextFieldShortPlaygroundSlot.Suffix
        val demoTextStyle =
            if (expandWidth && !hasSuffix) {
                AfternoteDesign.typography.textField.copy(
                    color = AfternoteDesign.colors.gray9,
                    textAlign = TextAlign.Center,
                )
            } else {
                null
            }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "TextFieldShort playground",
                style = AfternoteDesign.typography.h3,
                color = AfternoteDesign.colors.gray9,
            )

            TextFieldShort(
                state = fieldState,
                label = if (showLabel) "아이디" else null,
                placeholder = placeholderText,
                isError = isError,
                expandTextAreaToAvailableWidth = expandWidth,
                showOutline = showOutline,
                textStyle = demoTextStyle,
                prefix =
                    if (slot == TextFieldShortPlaygroundSlot.Prefix) {
                        {
                            Text(
                                text = "@",
                                style = AfternoteDesign.typography.textField,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }
                    } else {
                        null
                    },
                suffix =
                    if (slot == TextFieldShortPlaygroundSlot.Suffix) {
                        {
                            Text(
                                text = ".com",
                                style = AfternoteDesign.typography.textField,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }
                    } else {
                        null
                    },
                trailingContent =
                    if (slot == TextFieldShortPlaygroundSlot.Trailing) {
                        {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AfternoteDesign.colors.gray9,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    } else {
                        null
                    },
            )

            Text(
                text = "Controls",
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = fieldState.text.toString(),
                onValueChange = { new ->
                    fieldState.edit { replace(0, length, new) }
                },
                label = { Text("값 (필드와 동기화)") },
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = placeholderText,
                onValueChange = { placeholderText = it },
                label = { Text("Placeholder") },
                singleLine = true,
            )

            TextFieldShortPlaygroundLabeledCheckbox(showLabel, { showLabel = it }, "라벨 표시 (아이디)")
            TextFieldShortPlaygroundLabeledCheckbox(isError, { isError = it }, "isError (빨간 테두리)")
            TextFieldShortPlaygroundLabeledCheckbox(
                expandWidth,
                { expandWidth = it },
                "expandTextAreaToAvailableWidth",
            )
            TextFieldShortPlaygroundLabeledCheckbox(
                showOutline,
                { showOutline = it },
                "showOutline",
            )

            Text(
                text = "슬롯 (하나만)",
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )

            TextFieldShortPlaygroundSlotRow(
                selected = slot == TextFieldShortPlaygroundSlot.None,
                onClick = { slot = TextFieldShortPlaygroundSlot.None },
                title = "없음",
            )
            TextFieldShortPlaygroundSlotRow(
                selected = slot == TextFieldShortPlaygroundSlot.Prefix,
                onClick = { slot = TextFieldShortPlaygroundSlot.Prefix },
                title = "Prefix (@)",
            )
            TextFieldShortPlaygroundSlotRow(
                selected = slot == TextFieldShortPlaygroundSlot.Suffix,
                onClick = { slot = TextFieldShortPlaygroundSlot.Suffix },
                title = "Suffix (.com)",
            )
            TextFieldShortPlaygroundSlotRow(
                selected = slot == TextFieldShortPlaygroundSlot.Trailing,
                onClick = { slot = TextFieldShortPlaygroundSlot.Trailing },
                title = "Trailing (검색 아이콘)",
            )

            if (expandWidth && hasSuffix) {
                Text(
                    text = "참고: suffix가 있으면 실제 컴포넌트는 가로 확장을 적용하지 않습니다.",
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        }
    }
}
