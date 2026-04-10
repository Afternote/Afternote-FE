package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteActionButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun TodayQuestionCard(
    modifier: Modifier = Modifier,
    dateText: String = "2026.04.10",
    questionText: String = "오늘 하루,\n무엇이 가장 고마웠나요?",
    onAnswerClick: () -> Unit = {},
) {
    val gradientColors =
        listOf(
            Color(0xFFF7F9F7),
            Color(0xFFDCEBDE),
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(gradientColors)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            // 헤더 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TODAY'S QUESTION",
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

            // 질문 텍스트
            Text(
                text = questionText,
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.black,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 답변하기 버튼
            AfternoteActionButton(
                text = "데일리질문 답변하기",
                containerColor = AfternoteDesign.colors.gray9,
                onClick = onAnswerClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayQuestionCardPreview() {
    AfternoteTheme {
        TodayQuestionCard()
    }
}
