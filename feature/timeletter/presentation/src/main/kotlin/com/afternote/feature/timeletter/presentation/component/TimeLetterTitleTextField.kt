package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun TimeLetterTitleTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        state = state,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle =
            AfternoteDesign.typography.h2.copy(
                color = AfternoteDesign.colors.gray9,
            ),
        cursorBrush = SolidColor(AfternoteDesign.colors.black),
        decorator = { innerTextField ->
            Box {
                if (state.text.isEmpty()) {
                    Text(
                        text = "제목을 입력하세요",
                        style = AfternoteDesign.typography.h2,
                        color = AfternoteDesign.colors.gray3,
                    )
                }
                innerTextField()
            }
        },
    )
}
