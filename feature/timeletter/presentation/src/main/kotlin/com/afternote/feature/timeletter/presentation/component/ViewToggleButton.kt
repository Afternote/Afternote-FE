package com.afternote.feature.timeletter.presentation.component

import android.R.attr.contentDescription
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.feature.timeletter.presentation.viewmodel.ViewMode
import com.afternote.feature.timeletter.res.R

@Composable
fun ViewToggleButton(
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        Modifier
            .width(68.dp)
            .height(36.dp)
            .background(color = Color(0xFFEEEEEE), shape = RoundedCornerShape(size = 16777200.dp))
            .padding(start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp),
        ) {
            if (viewMode == ViewMode.List) {
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .background(color = Color.White, shape = CircleShape),
                )
            }
            Image(
                painterResource(com.afternote.feature.timeletter.presentation.R.drawable.ic_list),
                contentDescription = "리스트형",
                modifier =
                    Modifier
                        .size(28.dp)
                        .clickable {
                            onViewModeChange(ViewMode.List)
                        },
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp),
        ) {
            if (viewMode == ViewMode.Block) {
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .background(color = Color.White, shape = CircleShape),
                )
            }
            Image(
                painterResource(com.afternote.feature.timeletter.presentation.R.drawable.ic_block),
                contentDescription = "블록형",
                modifier =
                    Modifier
                        .size(20.dp)
                        .clickable {
                            onViewModeChange(ViewMode.Block)
                        },
            )
        }
    }
}

@Preview
@Composable
private fun ViewToggleButtonPrev() {
    ViewToggleButton(
        viewMode = ViewMode.List,
        onViewModeChange = {},
    )
}

@Preview
@Composable
private fun ViewToggleButtonPrevB() {
    ViewToggleButton(
        viewMode = ViewMode.Block,
        onViewModeChange = {},
    )
}
