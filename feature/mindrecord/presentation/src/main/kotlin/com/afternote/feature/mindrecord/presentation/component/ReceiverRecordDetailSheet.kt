package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.mapper.toEmoji
import com.afternote.feature.mindrecord.presentation.util.htmlToPlainText

/**
 * 수신자가 받은 기록 하나의 본문을 읽는 시트 (#618).
 *
 * 카드를 탭해도 아무 일이 없어 **제목 목록에서 멈춰** 있었다. 수신자가 고인의 기록을
 * 읽는 것이 이 화면의 목적인데 본문·기분에 도달할 경로가 없었다.
 *
 * **추가 조회를 하지 않는다.** 목록 응답이 이미 `content` 와 `todayMood` 를 함께 주므로
 * (`GET receiver-auth/diary` 실측), 화면이 들고 있는 항목을 그대로 펼치면 된다.
 *
 * 별도 라우트가 아니라 시트인 이유: 이 화면에는 뒤로가기가 없어(#614) 새 화면을 쌓으면
 * 나올 방법이 시스템 백키뿐이다. 시트는 스와이프·바깥 탭으로 닫힌다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverRecordDetailSheet(
    record: MindRecordSummary,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AfternoteDesign.colors.white,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = record.createdAt,
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
                // 일기에만 있는 값이라 없으면 자리도 만들지 않는다.
                record.todayMood?.let { mood ->
                    Text(text = mood.toEmoji(), style = AfternoteDesign.typography.captionLargeR)
                }
            }

            Text(
                text = record.title,
                style = AfternoteDesign.typography.bodyLargeB,
                color = AfternoteDesign.colors.gray9,
            )

            // 본문은 HTML 조각이라 태그를 벗겨 읽을 수 있는 텍스트로 만든다.
            Text(
                text = record.content.htmlToPlainText(),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReceiverRecordDetailSheetPreview() {
    AfternoteTheme {
        ReceiverRecordDetailSheet(
            record =
                MindRecordSummary(
                    id = 11L,
                    type = MindRecordType.DIARY,
                    title = "오늘의 산책",
                    content = "<p>강변을 한 시간 걸었다.</p>",
                    recordDate = "2026-07-29",
                    isDraft = false,
                    createdAt = "2026.07.29 수",
                    todayMood = TodayMood.HAPPY,
                ),
            onDismiss = {},
        )
    }
}
