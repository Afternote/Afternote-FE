package com.afternote.feature.mindrecord.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import coil3.compose.AsyncImage
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.util.htmlToPlainText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * 데일리 질문 답변 리스트 카드 (Figma 2757:16130 텍스트형 / 2757:16129 이미지형).
 *
 * @param recipientNames 받는 사람 이름 목록 텍스트 (예: "박채연, 000,"). null 이면 표시하지 않음
 * @param imageUrl 첨부 이미지 URL. null 이 아니면 좌측 썸네일이 있는 이미지형 카드로 렌더링
 */
@Composable
fun DailyQuestionListCard(
    answer: DailyQuestion,
    modifier: Modifier = Modifier,
    recipientNames: String? = null,
    imageUrl: String? = null,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    OutlinedCard(
        shape = RoundedCornerShape(6.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = AfternoteDesign.colors.white,
            ),
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (imageUrl != null) {
            // Figma 2757:16129 — 이미지형: p=12 / 썸네일 85 / gap=8
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp),
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(85.dp)
                            .clip(RoundedCornerShape(2.dp)),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    CardHeaderRow(
                        date = answer.date,
                        recipientNames = recipientNames,
                        onEdit = onEdit,
                        onDelete = onDelete,
                    )
                    Text(
                        text = answer.title,
                        style = AfternoteDesign.typography.bodySmallB,
                        color = AfternoteDesign.colors.gray9,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = answer.content.htmlToPlainText(),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray5,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            // Figma 2757:16130 — 텍스트형: p=16 / 날짜·받는사람 → 질문(Bold) → 답변 미리보기
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                CardHeaderRow(
                    date = answer.date,
                    recipientNames = recipientNames,
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = answer.title,
                    style = AfternoteDesign.typography.bodySmallB,
                    color = AfternoteDesign.colors.gray9,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = answer.content.htmlToPlainText(),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        }
    }
}

@Composable
private fun CardHeaderRow(
    date: LocalDate,
    recipientNames: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateText = remember(date) { date.format(DateFormatter) }
    val dayOfWeekText = stringResource(dayOfWeekLabelRes(date.dayOfWeek))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "$dateText $dayOfWeekText",
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
        if (recipientNames != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = recipientNames,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        Box {
            Icon(
                painter = painterResource(R.drawable.mindrecord_horizontal),
                tint = AfternoteDesign.colors.gray5,
                contentDescription = stringResource(R.string.mindrecord_more_menu_cd),
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
}

@StringRes
private fun dayOfWeekLabelRes(dayOfWeek: DayOfWeek): Int =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> R.string.mindrecord_calendar_day_label_mon
        DayOfWeek.TUESDAY -> R.string.mindrecord_calendar_day_label_tue
        DayOfWeek.WEDNESDAY -> R.string.mindrecord_calendar_day_label_wed
        DayOfWeek.THURSDAY -> R.string.mindrecord_calendar_day_label_thu
        DayOfWeek.FRIDAY -> R.string.mindrecord_calendar_day_label_fri
        DayOfWeek.SATURDAY -> R.string.mindrecord_calendar_day_label_sat
        DayOfWeek.SUNDAY -> R.string.mindrecord_calendar_day_label_sun
    }

@Preview(showBackground = true)
@Composable
private fun DailyQuestionCardPreview() {
    AfternoteTheme {
        DailyQuestionListCard(
            answer =
                DailyQuestion(
                    title = "오늘 하루, 누구에게 가장 고마웠나요?",
                    content = "아무 말 없이 그저 나의 곁을 지켜주는 아내가 고맙다.",
                    date = LocalDate.now(),
                ),
            recipientNames = "박채연, 000,",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionCardImagePreview() {
    AfternoteTheme {
        DailyQuestionListCard(
            answer =
                DailyQuestion(
                    title = "채연아 20번째 생일을 축하해",
                    content = "너가 태어난 게 엊그제같은데 벌써 스무살이라니..엄마가 없어도 씩씩하게 컸을 채연이를 상상하면 너무 기특해서 안아주고 싶구나",
                    date = LocalDate.now(),
                ),
            imageUrl = "https://example.com/image.png",
        )
    }
}
