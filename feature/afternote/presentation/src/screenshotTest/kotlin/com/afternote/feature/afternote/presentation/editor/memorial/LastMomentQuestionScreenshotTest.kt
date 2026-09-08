package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun lastMomentQuestionScreenshot() {
    AfternoteTheme {
        LastMomentQuestion(
            text = "가장 행복했던 순간을 떠올려 보세요.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
