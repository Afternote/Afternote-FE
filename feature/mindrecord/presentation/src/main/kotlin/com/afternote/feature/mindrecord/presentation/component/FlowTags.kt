package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.Gray1
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray6
import com.afternote.feature.mindrecord.presentation.model.Tag

@Composable
fun FlowTags(
    tags: List<Tag>,
    selectedTag: Tag?,
    onclick: () -> Unit,
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        FilterChip(
            selected = selectedTag == null,
            onClick = { onclick() },
            label = {
                Text(
                    "전체",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (selectedTag == null) Gray1 else Gray6,
                )
            },
            shape = RoundedCornerShape(16777200.dp),
            border = BorderStroke(1.dp, Gray2),
        )

        tags.forEach { tag ->
            FilterChip(
                selected = selectedTag == tag,
                onClick = { onTagClick(tag) },
                label = {
                    Text(
                        text = "#${tag.name} ${tag.count}",
                        style = MaterialTheme.typography.displayLarge,
                        color = if (selectedTag == tag) Gray1 else Gray6,
                    )
                },
                shape = RoundedCornerShape(16777200.dp),
                border = BorderStroke(1.dp, Gray2),
            )
        }
    }
}
