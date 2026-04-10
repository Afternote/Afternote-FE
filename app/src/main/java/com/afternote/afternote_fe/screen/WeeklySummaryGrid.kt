package com.afternote.afternote_fe.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun WeeklySummaryGrid(
    modifier: Modifier = Modifier,
    recordedCount: Int = 7,
    recentRecordDate: String = "26.02.01",
    recentRecordTitle: String = "깊은생각 제목깊은생각 제목깊...",
    onImageClick: () -> Unit = {},
    onCountCardClick: () -> Unit = {},
    onRecentRecordClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // [좌측] 이미지 카드 (1:1 정사각형 비율 유지)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onImageClick),
        ) {
            // TODO: 실제 구현 시 AsyncImage(Coil) 사용 권장
            Image(
                painter = painterResource(id = android.R.color.darker_gray),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // 텍스트 가독성을 위한 하단 그라데이션 오버레이
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, AfternoteDesign.colors.black.copy(alpha = 0.6f)),
                                startY = 150f,
                            ),
                        ),
            )

            Text(
                text = "RECORDED MOMENT",
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.white,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
            )
        }

        // [우측] 요약 카드 컨테이너
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // [우측 상단] 이번 주 기록 횟수 카드
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
                color = AfternoteDesign.colors.white,
                onClick = onCountCardClick,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "THIS WEEK",
                        style = AfternoteDesign.typography.mono,
                        color = AfternoteDesign.colors.gray5,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "기록된 순간들",
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray7,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = recordedCount.toString(),
                        style = AfternoteDesign.typography.h1,
                        color = AfternoteDesign.colors.black,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            // [우측 하단] 최근 깊은 생각 카드
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = AfternoteDesign.colors.gray8,
                onClick = onRecentRecordClick,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    // TODO: 실제 프로젝트의 달 아이콘 리소스로 교체 필요
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_myplaces),
                        contentDescription = null,
                        tint = AfternoteDesign.colors.white,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = recentRecordDate,
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = AfternoteDesign.colors.white.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recentRecordTitle,
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.white,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun WeeklySummaryGridPreview() {
    AfternoteTheme {
        WeeklySummaryGrid(
            modifier = Modifier.padding(24.dp),
        )
    }
}
