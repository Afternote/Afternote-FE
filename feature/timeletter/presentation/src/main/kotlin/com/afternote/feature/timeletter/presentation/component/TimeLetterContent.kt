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
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.viewmodel.ViewMode

@Composable
fun TimeLetterContent(
    letters: TimeLetterList,
    receiverNameMap: Map<Long, String>,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onWriteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (letters.timeLetters.isEmpty()) {
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
            Spacer(modifier= Modifier.weight(1f))
            ViewToggleButton(
                viewMode = viewMode,
                onViewModeChange = onViewModeChange,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        LazyColumn(
            modifier = Modifier.padding(top = 10.dp),
        ) {
            items(letters.timeLetters) { letter ->
                when (viewMode) {
                    ViewMode.List -> TimeLetterListItem(letter = letter, receiverNameMap = receiverNameMap)
                    ViewMode.Block -> TimeLetterBlockItem(letter = letter, receiverNameMap = receiverNameMap)
                }
            }
        }
    }
}

private val previewLetters =
    TimeLetterList(
        timeLetters =
            listOf(
                TimeLetter(
                    id = 1L,
                    title = "미래의 나에게",
                    sendAt = "2026-12-31T00:00:00",
                    deliveredAt = null,
                    status = TimeLetterStatus.SCHEDULED,
                    blocks = emptyList(),
                    receiverIds = listOf(1L),
                ),
                TimeLetter(
                    id = 2L,
                    title = "10년 후의 나에게",
                    sendAt = "2035-01-01T00:00:00",
                    deliveredAt = null,
                    status = TimeLetterStatus.SCHEDULED,
                    blocks = emptyList(),
                    receiverIds = listOf(2L),
                ),
            ),
        totalCount = 2,
    )

@Preview(showBackground = true)
@Composable
private fun TimeLetterContentView() {
    TimeLetterContent(
        letters = previewLetters,
        receiverNameMap = mapOf(1L to "박경민", 2L to "미래의 나"),
        viewMode = ViewMode.List,
        onViewModeChange = {},
    )
}
