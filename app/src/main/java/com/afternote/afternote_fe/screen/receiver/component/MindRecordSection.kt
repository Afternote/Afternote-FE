package com.afternote.afternote_fe.screen.receiver.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.afternote_fe.R
import com.afternote.afternote_fe.screen.receiver.model.MindRecordSummary
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.R as CoreUiR

/**
 * 마음의 기록 섹션 — 데일리 질문 / 일기 2개의 통계 카드를 함께 노출한다.
 */
@Composable
fun MindRecordSection(
    summary: MindRecordSummary,
    onGoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalLabel = stringResource(R.string.receiver_home_mindrecord_total_label)
    HomeSectionCard(
        modifier = modifier,
        title = stringResource(R.string.receiver_home_mindrecord_section_title),
        description = stringResource(R.string.receiver_home_mindrecord_section_desc),
        countLine =
            rememberCountLine(
                prefix = "${summary.totalCount}개 ",
                suffix = "마음의 기록이 있습니다.",
            ),
        buttonText = stringResource(R.string.receiver_home_mindrecord_section_button),
        onButtonClick = onGoClick,
        middleContent = {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MindRecordStatCard(
                    iconResId = CoreUiR.drawable.core_ui_ic_mindrecord,
                    label = stringResource(R.string.receiver_home_mindrecord_daily_question),
                    totalLabel = totalLabel,
                    count = summary.dailyQuestionCount,
                    modifier = Modifier.weight(1f),
                )
                MindRecordStatCard(
                    iconResId = CoreUiR.drawable.core_ui_ic_diary,
                    label = stringResource(R.string.receiver_home_mindrecord_diary),
                    totalLabel = totalLabel,
                    count = summary.diaryCount,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun MindRecordSectionPreview() {
    AfternoteTheme {
        MindRecordSection(
            summary =
                MindRecordSummary(
                    totalCount = 150,
                    dailyQuestionCount = 18,
                    diaryCount = 18,
                ),
            onGoClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
