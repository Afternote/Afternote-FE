package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteActionButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_today_question_answer_cta
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_today_question_header

private val TodayQuestionCardGradientTop = Color(0xFFF7F9F7)
private val TodayQuestionCardGradientBottom = Color(0xFFDCEBDE)

@Composable
fun TodayQuestionCard(
    modifier: Modifier = Modifier,
    dateText: String = "2026.04.10",
    questionText: String = "오늘 하루,\n무엇이 가장 고마웠나요?",
    onAnswerClick: () -> Unit = {},
) {
    val gradientBrush =
        remember {
            Brush.verticalGradient(
                listOf(TodayQuestionCardGradientTop, TodayQuestionCardGradientBottom),
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(gradientBrush)
                .padding(30.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(mindrecord_today_question_header),
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray5,
            )
            Text(
                text = dateText,
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray5,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = questionText,
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.black,
        )

        Spacer(modifier = Modifier.height(24.dp))

        AfternoteActionButton(
            text = stringResource(mindrecord_today_question_answer_cta),
            containerColor = AfternoteDesign.colors.gray9,
            onClick = onAnswerClick,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TodayQuestionCardPreview() {
    AfternoteTheme {
        TodayQuestionCard(modifier = Modifier.padding(16.dp))
    }
}
