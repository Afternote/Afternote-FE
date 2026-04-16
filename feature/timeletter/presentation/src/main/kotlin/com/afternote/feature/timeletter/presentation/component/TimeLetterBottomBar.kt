package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.timeletter.presentation.R

@Composable
fun TimeLetterBottomBar(
    draftCount: Int,
    onLinkClick: () -> Unit,
    onTextStyleClick: () -> Unit,
    onAlignCenterClick: () -> Unit,
    onAlignLeftClick: () -> Unit,
    onAlignRightClick: () -> Unit,
    onDraftClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onLinkClick) {
            Icon(
                painter = painterResource(R.drawable.ic_link),
                contentDescription = "링크 삽입",
                tint = AfternoteDesign.colors.gray7,
                modifier = Modifier.size(22.dp),
            )
        }

        IconButton(onClick = onTextStyleClick) {
            Icon(
                painter = painterResource(R.drawable.ic_text),
                contentDescription = "텍스트",
                tint = AfternoteDesign.colors.gray7,
                modifier = Modifier.size(22.dp),
            )
        }

        Row(
            modifier =
                Modifier
                    .width(106.dp)
                    .height(43.dp)
                    .background(color = AfternoteDesign.colors.gray2, shape = RoundedCornerShape(size = 20132640.dp))
                    .padding(start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlignButton(
                painter = painterResource(R.drawable.ic_align_center),
                selected = true,
                onClick = onAlignCenterClick,
            )
            AlignButton(
                painter = painterResource(R.drawable.ic_align_left),
                selected = false,
                onClick = onAlignLeftClick,
            )
            AlignButton(
                painter = painterResource(R.drawable.ic_align_right),
                selected = false,
                onClick = onAlignRightClick,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onDraftClick) {
            Text(
                text = "임시저장",
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray7,
            )
            if (draftCount > 0) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$draftCount",
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray7,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimeLetterBottomBarPreview() {
    TimeLetterBottomBar(
        draftCount = 3,
        onLinkClick = {},
        onTextStyleClick = {},
        onAlignCenterClick = {},
        onAlignLeftClick = {},
        onAlignRightClick = {},
        onDraftClick = {},
    )
}
