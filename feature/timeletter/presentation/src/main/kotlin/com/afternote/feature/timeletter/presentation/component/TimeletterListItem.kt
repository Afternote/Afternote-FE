package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.Gray6
import com.afternote.core.ui.theme.afternoteTypography
import com.afternote.feature.timeletter.domain.LetterIdentity
import com.afternote.feature.timeletter.domain.LetterSchedule
import com.afternote.feature.timeletter.domain.OpenDate
import com.afternote.feature.timeletter.domain.TimeLetter

@Composable
fun TimeletterListItem(
    letter: TimeLetter,
    modifier: Modifier = Modifier,
) {
    val identity = letter.identity
    val schedule = letter.schedule
    val typography = MaterialTheme.typography
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 19.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "수신인  ${schedule.recipientName}",
                style = typography.bodySmall,
                color = Gray6,
            )
            Spacer(modifier = Modifier.weight(1f)) // 👈 이게 핵심
            Text(
                text = "발송 예정일  ${schedule.openDate.value.replace("-", ".")}",
                style = typography.bodySmall,
                color = Gray6,
            )
            Spacer(modifier = Modifier.width(43.dp))
            Image(
                painterResource(com.afternote.feature.timeletter.presentation.R.drawable.setting),
                contentDescription = "더보기 설정",
            )
        }
        Spacer(modifier = Modifier.padding(top = 11.dp))
        Row {
            Column {
                Text(
                    text = identity.title,
                    style = afternoteTypography.bodyLarge,
                    fontWeight = FontWeight.W600,
                )
                Spacer(modifier = Modifier.padding(top = 5.dp))
                Text(
                    text = identity.body,
                    style = typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Gray6,
                )
            }
            Spacer(modifier = Modifier.padding(start = 10.dp))
            Image(
                painterResource(com.afternote.feature.timeletter.presentation.R.drawable.ex_box_img),
                contentDescription = "박스예시이미지",
                modifier = Modifier.size(55.dp),
            )
        }
    }
    HorizontalDivider()
}

@Preview(showBackground = true)
@Composable
private fun TimeletterItemPreview() {
    TimeletterListItem(
        letter =
            TimeLetter(
                identity =
                    LetterIdentity(
                        id = 1L,
                        title = "미래의 나에게",
                        body = "지금 이 순간을 잊지 마. 열심히 살고 있는 너를 응원해.",
                    ),
                schedule =
                    LetterSchedule(
                        recipientName = "박경민",
                        openDate = OpenDate("2026-12-31"),
                    ),
            ),
    )
}
