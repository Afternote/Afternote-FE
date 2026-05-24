package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus

@Composable
fun TimeLetterBlockItem(
    letter: TimeLetter,
    modifier: Modifier = Modifier,
    receiverNameMap: Map<Long, String> = emptyMap(),
) {
    Column(
        modifier =
            modifier
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray4,
                    shape = RoundedCornerShape(size = 6.dp),
                ).fillMaxWidth(),
    ) {
        Image(
            painterResource(com.afternote.feature.timeletter.presentation.R.drawable.container),
            contentDescription = "예시 블록",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(100.dp),
        )
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 15.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val receiverText =
                    letter.receiverIds
                        .mapNotNull { receiverNameMap[it] }
                        .joinToString(", ")
                        .ifEmpty { "${letter.receiverIds.size}명" }
                Text(
                    text = "수신인  $receiverText",
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray6,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "발송 예정일  ${letter.sendAt?.replace("-", ".") ?: ""}",
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray6,
                )
                Spacer(modifier = Modifier.width(43.dp))
                Image(
                    painterResource(com.afternote.feature.timeletter.presentation.R.drawable.setting),
                    contentDescription = "더보기 설정",
                )
            }
            Spacer(modifier = Modifier.padding(top = 7.dp))

            Text(
                text = letter.title ?: "제목 없음",
                style = AfternoteDesign.typography.bodyLargeB,
                fontWeight = FontWeight.W600,
            )
            Spacer(modifier = Modifier.padding(top = 5.dp))
            Text(
                text = letter.content ?: "",
                style = AfternoteDesign.typography.bodyLargeR,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = AfternoteDesign.colors.gray6,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeLetterBlockItemPrev() {
    TimeLetterBlockItem(
        letter =
            TimeLetter(
                id = 1L,
                title = "미래의 나에게",
                sendAt = "2026-12-31T00:00:00",
                deliveredAt = null,
                status = TimeLetterStatus.SCHEDULED,
                blocks = emptyList(),
                receiverIds = listOf(1L),
            ),
    )
}
