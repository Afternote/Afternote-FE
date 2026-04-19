package com.afternote.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 캡션 라벨 + 간격 + 입력 영역을 한 묶음으로 쌓는 공통 레이아웃.
 *
 * 제목·본문·아이디·비밀번호 등 동일한 상하 구조를 재사용할 때 사용합니다.
 */
@Composable
fun CaptionLabeledField(
    label: String,
    modifier: Modifier = Modifier,
    labelColor: Color = AfternoteDesign.colors.gray6,
    verticalSpacing: Dp = 6.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        Text(
            text = label,
            style = AfternoteDesign.typography.captionLargeR,
            color = labelColor,
        )
        content()
    }
}

/**
 * [CaptionLabeledField] 위에 [AfternoteTextField]를 올린 조합.
 */
@Composable
fun CaptionLabeledTextField(
    label: String,
    state: TextFieldState,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    labelColor: Color = AfternoteDesign.colors.gray6,
    verticalSpacing: Dp = 6.dp,
    type: TextFieldType = TextFieldType.Basic,
    placeholder: String = "Text Field",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    focusRequester: FocusRequester? = null,
) {
    CaptionLabeledField(
        label = label,
        modifier = modifier,
        labelColor = labelColor,
        verticalSpacing = verticalSpacing,
    ) {
        AfternoteTextField(
            state = state,
            modifier = fieldModifier,
            type = type,
            placeholder = placeholder,
            keyboardType = keyboardType,
            imeAction = imeAction,
            onImeAction = onImeAction,
            inputTransformation = inputTransformation,
            outputTransformation = outputTransformation,
            focusRequester = focusRequester,
        )
    }
}
