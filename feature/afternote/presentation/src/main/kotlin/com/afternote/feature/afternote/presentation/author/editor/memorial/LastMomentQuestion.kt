package com.afternote.feature.afternote.presentation.author.editor.memorial

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

/**
 * 마지막 순간 질문 텍스트 컴포넌트
 *
 * 피그마 디자인 기반:
 * - 텍스트: 16sp, Regular, AfternoteDesign.colors.gray9
 * - 라인 높이: 22sp
 */
@Composable
fun LastMomentQuestion(
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val questionText = text ?: stringResource(R.string.afternote_editor_last_moment_question_default)
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = questionText,
            style =
                AfternoteDesign.typography.textField.copy(
                    fontWeight = FontWeight.Medium,
                    color = AfternoteDesign.colors.gray9,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LastMomentQuestionPreview() {
    AfternoteTheme {
        LastMomentQuestion(
            modifier = Modifier.padding(20.dp),
        )
    }
}
