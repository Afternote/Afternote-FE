package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R

@Composable
fun MemoriesCard(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    color = AfternoteDesign.colors.gray3,
                    shape = RoundedCornerShape(8.dp),
                ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.mindrecord_img),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = 0.7f
                        }.drawWithContent {
                            drawContent()
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        colorStops =
                                            arrayOf(
                                                0.0f to Color(0xFF000000).copy(alpha = 0.0f),
                                                0.5f to Color(0xFFFFFFFF).copy(alpha = 0.2f),
                                                1.0f to Color(0xFFF8F8F7),
                                            ),
                                    ),
                            )
                        },
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "\"내 인생에서 가장 소중했던 순간은?\"",
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray8,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = "- 아이가 태어났을 때...",
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier =
                    Modifier
                        // 1. 테두리 (Border)
                        .border(
                            width = 1.dp,
                            color = AfternoteDesign.colors.gray3,
                            shape = RoundedCornerShape(20.dp),
                        )
                        // 2. 자르기 (Clip): Ripple 효과와 배경색이 모서리 밖으로 나가지 않게 가둠
                        .clip(RoundedCornerShape(20.dp))
                        // 3. 배경색 (Background)
                        .background(color = AfternoteDesign.colors.gray2)
                        // 4. 클릭 (Clickable): 접근성 Role.Button 추가
                        .clickable(
                            role = Role.Button,
                            onClick = {},
                        )
                        // 5. 내부 여백 (Padding): 기존 Row에 있던 패딩을 가장 마지막에 배치
                        .padding(
                            horizontal = 17.dp,
                            vertical = 9.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "그날의 기록 다시 읽기",
                    style = AfternoteDesign.typography.inter,
                    color = AfternoteDesign.colors.gray7,
                )

                Spacer(modifier = Modifier.width(9.dp))

                RightArrowIcon(
                    modifier = Modifier.size(width = 4.dp, height = 7.dp),
                    tint = AfternoteDesign.colors.gray5,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemoriesCardPreview() {
    AfternoteTheme {
        MemoriesCard()
    }
}
