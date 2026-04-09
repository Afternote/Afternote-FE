package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun TimeLetterBodyTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
) {
    val charCount = state.text.length

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .border(1.dp, AfternoteDesign.colors.gray2, RoundedCornerShape(8.dp)),
    ) {
        BasicTextField(
            state = state,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 320.dp)
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 36.dp),
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 10),
            textStyle =
                AfternoteDesign.typography.bodySmallR.copy(
                    color = AfternoteDesign.colors.gray9,
                ),
            cursorBrush = SolidColor(AfternoteDesign.colors.black),
            decorator = { innerTextField ->
                Box {
                    if (state.text.isEmpty()) {
                        Text(
                            text = "타임레터를 남겨 보세요",
                            style = AfternoteDesign.typography.bodySmallR,
                            color = AfternoteDesign.colors.gray4,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Text(
            text = "$charCount 글자",
            style = AfternoteDesign.typography.inter,
            color = AfternoteDesign.colors.gray4,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 10.dp),
        )
    }
}
