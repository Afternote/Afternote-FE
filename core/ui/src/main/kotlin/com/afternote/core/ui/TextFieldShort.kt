package com.afternote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
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
    /**
     * `true` 면 [suffix] 를 앞 텍스트 바로 뒤 8dp 에 붙이고 남는 폭을 오른쪽에 비운다.
     *
     * 기본값(`false`)은 앞 텍스트가 남는 폭을 전부 먹어 suffix 가 오른쪽 끝으로 밀리는 배치다 —
     * `Variant7`(인증번호 받기)·`Search` 는 그쪽이 시안이고, `Variant8`(주민번호) 만 앞에 붙는다.
     */
    suffixFollowsText: Boolean = false,
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
                        modifier =
                            if (suffixFollowsText) {
                                // 폭을 내용 크기로 고정한다. 없으면(= weight(1f) 기본값이면)
                                // 텍스트필드가 배정폭을 끝까지 채워 suffix 가 오른쪽 끝으로 밀린다.
                                Modifier.width(IntrinsicSize.Max)
                            } else {
                                Modifier.weight(1f)
                            },
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
        // else 로 닫지 않는다 — 타입이 늘면 후행 아이콘 여부를 여기서 정하도록 컴파일 에러로 잡는다.
        trailingContent =
            when (type) {
                TextFieldType.Search -> {
                    { SearchIcon() }
                }

                TextFieldType.Basic,
                is TextFieldType.Variant7,
                is TextFieldType.Variant8,
                -> {
                    null
                }
            },
        suffixFollowsText = type is TextFieldType.Variant8,
        // 위와 같은 이유로 else 를 두지 않는다.
        suffix =
            when (type) {
                is TextFieldType.Variant7 -> {
                    { Variant7Suffix(type) }
                }

                is TextFieldType.Variant8 -> {
                    { Variant8Suffix(type = type, onImeAction = onImeAction) }
                }

                TextFieldType.Basic,
                TextFieldType.Search,
                -> {
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

@Composable
private fun Variant8Suffix(
    type: TextFieldType.Variant8,
    onImeAction: (() -> Unit)?,
) {
    val backInputContentDescription =
        stringResource(R.string.core_ui_content_description_resident_number_back_input)

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
            textStyle = AfternoteDesign.typography.textField.copy(color = AfternoteDesign.colors.black),
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
        Variant8MaskDots(dotCount = type.dotCount)
    }
}

@Composable
private fun Variant8MaskDots(dotCount: Int) {
    Row(
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
