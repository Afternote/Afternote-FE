package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.util.htmlToPlainText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

private val DiaryCardDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * 일기 카드 형 그리드 셀. Figma 2671:16748 / 16766 / 16779.
 *
 * - 사진이 있으면: 128dp 이미지 + 하단 그라데이션 스크림 + 좌하단 감정 이모지 오버레이
 * - 사진이 없고 감정만 있으면: 그라데이션 배경에 감정 이모지(32sp) 플레이스홀더
 * - 둘 다 없으면: 텍스트 전용 카드
 */
@Composable
fun DiaryCard(
    diary: DailyDiary,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = AfternoteDesign.colors.white,
            ),
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            val imageUrl = diary.imageUrl
            val emotion = diary.emotion
            if (imageUrl != null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(128.dp),
                ) {
                    Image(
                        painter = painterResource(imageUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color(0xFF000000).copy(alpha = 0.4f),
                                            ),
                                    ),
                                ),
                    )
                    emotion?.let {
                        Text(
                            text = it,
                            fontSize = 20.sp,
                            modifier =
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                        )
                    }
                }
            } else if (emotion != null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                            .background(
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            Color(0xFFFAFAF9),
                                            Color(0xFFF5F5F4),
                                        ),
                                ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emotion,
                        fontSize = 32.sp,
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = diary.date.format(DiaryCardDateFormatter),
                            style = AfternoteDesign.typography.footnoteCaption,
                            color = AfternoteDesign.colors.gray6,
                        )
                        Text(
                            text = diary.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.KOREAN),
                            style = AfternoteDesign.typography.footnoteCaption,
                            color = AfternoteDesign.colors.gray6,
                        )
                    }

                    Box {
                        Icon(
                            painter = painterResource(R.drawable.mindrecord_horizontal),
                            contentDescription = stringResource(R.string.mindrecord_more_menu_cd),
                            tint = AfternoteDesign.colors.gray6,
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .clickable { menuExpanded = true },
                        )
                        if (menuExpanded) {
                            RecordActionPopup(
                                onDismiss = { menuExpanded = false },
                                onDelete = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                onEdit = {
                                    menuExpanded = false
                                    onEdit()
                                },
                            )
                        }
                    }
                }

                Text(
                    text = diary.title,
                    style = AfternoteDesign.typography.bodySmallB,
                    color = AfternoteDesign.colors.gray9,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = diary.content.htmlToPlainText(),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryCardPreview() {
    AfternoteTheme {
        DiaryCard(
            diary =
                DailyDiary(
                    title = "가족과 함께한 저녁 식사",
                    content = "오랜만에 가족들과 둘러앉아 이야기를 나누는 시간이 정말 소중했다.",
                    date = LocalDate.now(),
                    emotion = "😊",
                    imageUrl = R.drawable.mindrecord_img,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryCardNoImagePreview() {
    AfternoteTheme {
        DiaryCard(
            diary =
                DailyDiary(
                    title = "새로운 취미 시작",
                    content = "수채화 그리기를 시작했다. 서툴지만 무언가에 집중하는 시간이 좋다.",
                    date = LocalDate.now(),
                    emotion = "😌",
                ),
        )
    }
}
