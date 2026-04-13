package com.afternote.feature.mindrecord.presentation.component.memoryspace

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.afternote.core.ui.icon.CloseIcon
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.modifierextention.shimmerLoadingPlaceholder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem

@Composable
fun MemoryDetailOverlay(
    memory: MemoryItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClose() })
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 13.dp, vertical = 37.dp)
                    .pointerInput(Unit) { detectTapGestures {} }
                    .clip(RoundedCornerShape(12.dp))
                    .background(AfternoteDesign.colors.white),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                SubcomposeAsyncImage(
                    model = memory.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .shimmerLoadingPlaceholder(),
                        )
                    },
                )

                IconButton(
                    onClick = onClose,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                            .shadow(10.dp, CircleShape)
                            .background(AfternoteDesign.colors.white, CircleShape)
                            .size(36.dp),
                ) {
                    CloseIcon(
                        modifier = Modifier.size(20.dp),
                        tint = AfternoteDesign.colors.gray7,
                    )
                }
            }

            Column(modifier = Modifier.padding(32.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = memory.title,
                        style =
                            AfternoteDesign.typography.inter.copy(
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.Light,
                            ),
                        color = AfternoteDesign.colors.gray9,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    RightArrowIcon(
                        modifier = Modifier.size(width = 6.dp, height = 11.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = memory.date,
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray5,
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = AfternoteDesign.colors.gray4,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = memory.content,
                    style =
                        AfternoteDesign.typography.inter.copy(
                            fontSize = 14.sp,
                            lineHeight = 23.sp,
                        ),
                    color = AfternoteDesign.colors.gray7,
                )

                Spacer(modifier = Modifier.height(15.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    memory.tags.forEach { tag ->
                        Text(
                            text = "#$tag",
                            modifier =
                                Modifier
                                    .background(
                                        AfternoteDesign.colors.gray4,
                                        RoundedCornerShape(16.dp),
                                    ).padding(horizontal = 12.dp, vertical = 6.dp),
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.gray6,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemoryDetailOverlayPreview() {
    AfternoteTheme {
        MemoryDetailOverlay(
            memory =
                MemoryItem(
                    id = 1,
                    imageUrl = "https://picsum.photos/400/600?random=1",
                    title = "오늘의 기억",
                    date = "2024.11.11",
                    content = "오늘은 소중한 사람들과 함께 보낸 행복한 하루였다. 날씨도 맑고 기분도 좋아서 오랫동안 기억에 남을 것 같다.",
                    tags = listOf("일상", "행복", "기억"),
                ),
            onClose = {},
        )
    }
}
