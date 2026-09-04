package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.feature.home.presentation.R
import com.afternote.feature.home.presentation.receiver.model.MindRecordSummary
import com.afternote.core.ui.R as CoreUiR

/**
 * 마음의 기록 섹션 — 데일리 질문 / 일기 2개의 통계 카드를 함께 노출한다.
 */
@Composable
fun MindRecordSection(
    summary: MindRecordSummary?,
    onGoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalLabel = stringResource(R.string.home_receiver_mindrecord_total_label)
    HomeSectionCard(
        modifier = modifier,
        title = stringResource(R.string.home_receiver_mindrecord_section_title),
        description = stringResource(R.string.home_receiver_mindrecord_section_desc),
        countLine =
            rememberCountLine(
                prefix =
                    stringResource(R.string.home_receiver_mindrecord_count_prefix, countText(summary?.totalCount)),
                suffix = stringResource(R.string.home_receiver_mindrecord_count_suffix),
            ),
        buttonText = stringResource(R.string.home_receiver_mindrecord_section_button),
        onButtonClick = onGoClick,
        middleContent = {
            // 조회 실패에도 카드를 숨기지 않는다 — 섹션 레이아웃 유지가 시안 확정값이고,
            // 통째로 사라지면 «기능이 없는 것» 과 «못 불러온 것» 이 구분되지 않는다 (#952).
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MindRecordStatCard(
                    iconResId = CoreUiR.drawable.core_ui_ic_mindrecord,
                    label = stringResource(R.string.home_receiver_mindrecord_daily_question),
                    totalLabel = totalLabel,
                    count = summary?.dailyQuestionCount,
                    modifier = Modifier.weight(1f),
                )
                MindRecordStatCard(
                    iconResId = CoreUiR.drawable.core_ui_ic_diary,
                    label = stringResource(R.string.home_receiver_mindrecord_diary),
                    totalLabel = totalLabel,
                    count = summary?.diaryCount,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}
