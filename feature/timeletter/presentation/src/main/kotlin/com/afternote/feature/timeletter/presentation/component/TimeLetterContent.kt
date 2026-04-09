package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.timeletter.domain.LetterIdentity
import com.afternote.feature.timeletter.domain.LetterSchedule
import com.afternote.feature.timeletter.domain.OpenDate
import com.afternote.feature.timeletter.domain.Recipient
import com.afternote.feature.timeletter.domain.TimeLetter
import com.afternote.feature.timeletter.domain.TimeLetters

@Composable
fun TimeLetterContent(
    letters: TimeLetters,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) {
        EmptyTimeLetterContent(modifier = modifier)
        return
    }

    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Text("타임레터", style = AfternoteDesign.typography.h1)
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            "소중한 사람들을 위한 마음을 남겨보세요",
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )

        Spacer(modifier = Modifier.padding(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .width(94.dp)
                    .height(36.dp)
                    .background(
                        color = Color(0xFFEEEEEE),
                        shape = RoundedCornerShape(size = 16777200.dp),
                    ).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "전체레터",
                    style = AfternoteDesign.typography.captionLargeR,
                )
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painterResource(com.afternote.feature.timeletter.presentation.R.drawable.down_vector),
                    contentDescription = "아래열기",
                    modifier =
                        Modifier
                            .width(8.dp)
                            .height(4.dp),
                )
            }
            ViewToggleButton(
                viewMode = viewMode,
                onViewModeChange = onViewModeChange,
            )
        }
        Spacer(modifier = Modifier.padding(21.dp))
        LazyColumn(
            modifier = Modifier.padding(top = 10.dp),
        ) {
            items(letters.toList()) { letter ->
                when (viewMode) {
                    ViewMode.List -> TimeLetterListItem(letter = letter)
                    ViewMode.Block -> TimeLetterBlockItem(letter = letter)
                }
            }
        }
    }
}

private val previewLetters =
    TimeLetters(
        listOf(
            TimeLetter(
                identity =
                    LetterIdentity(
                        id = 1L,
                        title = "미래의 나에게",
                        body = "지금 이 순간을 잊지 마. 열심히 살고 있는 너를 응원해.",
                    ),
                schedule = LetterSchedule(recipient = Recipient(id = 1L, name = "박경민", relationship = "친구"), openDate = OpenDate("2026-12-31")),
            ),
            TimeLetter(
                identity = LetterIdentity(id = 2L, title = "10년 후의 나에게", body = "지금보다 더 행복하길 바라."),
                schedule =
                    LetterSchedule(
                        recipient = Recipient(id = 2L, name = "미래의 나", relationship = "나"),
                        openDate = OpenDate("2035-01-01"),
                    ),
            ),
        ),
    )

@Preview(showBackground = true)
@Composable
private fun TimeLetterContentView() {
    TimeLetterContent(
        letters = previewLetters,
        viewMode = ViewMode.List,
        onViewModeChange = {},
    )
}
