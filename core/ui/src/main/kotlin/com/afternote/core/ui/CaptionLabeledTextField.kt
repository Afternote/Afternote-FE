package com.afternote.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 캡션 라벨 + 8dp 간격 + [AfternoteTextField] 조합 공통 컴포넌트.
 *
 * 제목·아이디·비밀번호 등 표준 입력 케이스를 짧게 표현하기 위해 사용합니다.
 * 멀티라인 등 [AfternoteTextField] 시그니처를 벗어나는 입력은 호출처에서 직접 조립하세요.
 */
@Composable
fun CaptionLabeledTextField(
    label: String,
    state: TextFieldState,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
        AfternoteTextField(
            state = state,
            keyboardType = keyboardType,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CaptionLabeledTextFieldPreview() {
    AfternoteTheme {
        CaptionLabeledTextField(
            label = "Label",
            state = rememberTextFieldState(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
