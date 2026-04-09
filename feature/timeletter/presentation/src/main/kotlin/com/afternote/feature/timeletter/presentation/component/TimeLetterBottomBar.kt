package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign

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
    Surface(
        shadowElevation = 8.dp,
        color = AfternoteDesign.colors.white,
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onLinkClick) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_share),
                    contentDescription = "링크 삽입",
                    tint = AfternoteDesign.colors.gray7,
                    modifier = Modifier.size(22.dp),
                )
            }

            IconButton(onClick = onTextStyleClick) {
                Text(
                    text = "T",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                    color = AfternoteDesign.colors.gray7,
                )
            }

            Row(
                modifier =
                    Modifier
                        .background(AfternoteDesign.colors.gray1, RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                AlignButton(label = "≡", selected = true, onClick = onAlignCenterClick)
                AlignButton(label = "≡", selected = false, onClick = onAlignLeftClick)
                AlignButton(label = "≡", selected = false, onClick = onAlignRightClick)
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onDraftClick) {
                Text(
                    text = "임시저장",
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray7,
                )
                if (draftCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$draftCount",
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray7,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlignButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .then(
                    if (selected) {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(AfternoteDesign.colors.white)
                    } else {
                        Modifier
                    },
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 14.sp),
            color = if (selected) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray5,
        )
    }
}
