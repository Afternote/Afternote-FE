// [WIP / 소속 미확정] MemorySpace — UI 컴포넌트(상세 오버레이).
// 경로: feature/mindrecord/presentation/.../component/memoryspace/

package com.afternote.feature.mindrecord.presentation.component.memoryspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
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
                // 뒷배경은 루트 Box의 Modifier.blur()로 뿌옇게 처리되므로 오버레이 자체는 투명 처리
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            shape = RoundedCornerShape(12.dp),
            color = AfternoteDesign.colors.white,
        ) {
            Column {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                ) {
                    Image(
                        painter = painterResource(id = memory.imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    Surface(
                        onClick = onClose,
                        shape = CircleShape,
                        color = AfternoteDesign.colors.white,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp),
                                tint = AfternoteDesign.colors.gray7,
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = memory.title,
                            style = AfternoteDesign.typography.h3,
                            color = AfternoteDesign.colors.gray9,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = AfternoteDesign.colors.gray6,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = memory.date,
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray5,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = AfternoteDesign.colors.gray3,
                    )

                    Text(
                        text = memory.content,
                        style = AfternoteDesign.typography.bodyBase,
                        color = AfternoteDesign.colors.gray7,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        memory.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AfternoteDesign.colors.gray3,
                            ) {
                                Text(
                                    text = "#$tag",
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp,
                                        ),
                                    style = AfternoteDesign.typography.captionLargeR,
                                    color = AfternoteDesign.colors.gray6,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
