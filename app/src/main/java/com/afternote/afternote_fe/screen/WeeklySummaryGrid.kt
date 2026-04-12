package com.afternote.afternote_fe.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.R
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
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = 8.dp
        // 수학적 계산: 전체 너비를 3등분하여 작은 사각형의 기준 사이즈를 구함
        val smallSquareSize = (maxWidth - (gap * 2)) / 3
        val largeSquareSize = (smallSquareSize * 2) + gap

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            // [좌측] 큰 정사각형 카드 (RECORDED MOMENT)
            Box(
                modifier =
                    Modifier
                        .size(largeSquareSize)
                        .clip(RoundedCornerShape(6.dp)),
            ) {
                Image(
                    painter = painterResource(R.drawable.core_ui_img_recorded_moment),
                    contentDescription = "recorded moment",
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clickable(onClick = onImageClick),
                )
                Text(
                    text = "RECORDED MOMENT",
                    style = AfternoteDesign.typography.mono,
                    color = AfternoteDesign.colors.white,
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                )
            }

            // [우측] 작은 정사각형 카드 2개를 담은 컬럼
            Column(
                modifier = Modifier.height(largeSquareSize), // 왼쪽 큰 카드와 높이를 동일하게 맞춤
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                // [우측 상단] 이번 주 기록 횟수 카드
                Surface(
                    modifier = Modifier.size(smallSquareSize),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
                    color = AfternoteDesign.colors.white,
                    onClick = onCountCardClick,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "THIS WEEK",
                            style = AfternoteDesign.typography.mono,
                            color = AfternoteDesign.colors.gray6,
                        )
                        Text(
                            text = "기록된 순간들",
                            style =
                                AfternoteDesign.typography.footnoteCaption.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 0.005.em,
                                ),
                            color = AfternoteDesign.colors.gray6,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = recordedCount.toString(),
                            style =
                                AfternoteDesign.typography.inter.copy(
                                    fontSize = 24.sp,
                                    lineHeight = 36.sp,
                                    letterSpacing = 0.003.em,
                                ),
                            color = AfternoteDesign.colors.black,
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }

                // [우측 하단] 최근 깊은 생각 카드
                Surface(
                    modifier = Modifier.size(smallSquareSize),
                    shape = RoundedCornerShape(6.dp),
                    color = AfternoteDesign.colors.gray8,
                    onClick = onRecentRecordClick,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .padding(top = 17.dp, bottom = 12.dp)
                                .padding(horizontal = 17.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.core_ui_ic_deep_thought_moon), // 프로젝트 리소스
                            contentDescription = null,
                            tint = AfternoteDesign.colors.white,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = recentRecordDate,
                            style = AfternoteDesign.typography.mono,
                            color = AfternoteDesign.colors.white,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = recentRecordTitle,
                            style =
                                AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.white,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklySummaryGridPreview() {
    AfternoteTheme {
        WeeklySummaryGrid()
    }
}
