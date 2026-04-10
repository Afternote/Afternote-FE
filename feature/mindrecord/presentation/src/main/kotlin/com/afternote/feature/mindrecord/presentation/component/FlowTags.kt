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
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
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
                    style = AfternoteDesign.typography.captionLargeB,
                    color = if (selectedTag == null) AfternoteDesign.colors.gray1 else AfternoteDesign.colors.gray6,
                )
            },
            shape = RoundedCornerShape(16777200.dp),
            border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
        )

        tags.forEach { tag ->
            FilterChip(
                selected = selectedTag == tag,
                onClick = { onTagClick(tag) },
                label = {
                    Text(
                        text = "#${tag.name} ${tag.count}",
                        style = AfternoteDesign.typography.captionLargeB,
                        color = if (selectedTag == tag) AfternoteDesign.colors.gray1 else AfternoteDesign.colors.gray6,
                    )
                },
                shape = RoundedCornerShape(16777200.dp),
                border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
            )
        }
    }
}
