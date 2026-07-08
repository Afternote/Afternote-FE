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
import androidx.compose.ui.draw.clip
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

private val DiaryComponentDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * 일기 캘린더 형 하단 기록 카드. Figma 2671:17691.
 *
 * 좌측 85dp 썸네일, 우측 날짜·요일·감정 이모지 + 더보기, 제목, 2줄 말줄임 본문.
 */
@Composable
fun DiaryComponent(
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(85.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(AfternoteDesign.colors.gray1),
            ) {
                Image(
                    painter = painterResource(diary.imageUrl ?: R.drawable.mindrecord_img),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = diary.date.format(DiaryComponentDateFormatter),
                                style = AfternoteDesign.typography.captionLargeR,
                                color = AfternoteDesign.colors.gray6,
                            )
                            Text(
                                text = diary.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.KOREAN),
                                style = AfternoteDesign.typography.captionLargeR,
                                color = AfternoteDesign.colors.gray6,
                            )
                        }
                        diary.emotion?.let {
                            Text(
                                text = it,
                                fontSize = 18.sp,
                            )
                        }
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
                    color = AfternoteDesign.colors.gray5,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryComponentPreview() {
    AfternoteTheme {
        DiaryComponent(
            diary =
                DailyDiary(
                    title = "채연아 20번째 생일을 축하해",
                    content = "너가 태어난 게 엊그제같은데 벌써 스무살이라니.. 엄마가 없어도 씩씩하게 컸을 채연이를 상상하면 너무 기특해서 안아주고 싶구나",
                    date = LocalDate.now(),
                    emotion = "😊",
                ),
        )
    }
}
