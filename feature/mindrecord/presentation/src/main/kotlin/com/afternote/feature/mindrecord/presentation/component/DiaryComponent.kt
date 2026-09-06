package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.util.htmlToPlainText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val DateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * Figma 2671:17678 — 일기 캘린더 형 리스트 카드: 썸네일(있을 때) + 날짜·요일·이모지 + 제목 + 내용 2줄.
 */
@Composable
fun DiaryComponent(
    diary: DailyDiary,
    modifier: Modifier = Modifier,
    /** 카드 전체 탭 — 저장된 기록 본문을 여는 상세 화면으로 간다 (#759). */
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = AfternoteDesign.colors.white,
            ),
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        modifier = modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (diary.imageUrl != null) {
                AsyncImage(
                    model = diary.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(85.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(AfternoteDesign.colors.gray1),
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = diary.date.format(DateFormatter),
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.gray6,
                        )
                        Text(
                            text = diary.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.gray6,
                        )
                        diary.emotion?.let { Text(text = it) }
                    }

                    Box {
                        Icon(
                            painter = painterResource(R.drawable.mindrecord_horizontal),
                            tint = AfternoteDesign.colors.gray5,
                            contentDescription = stringResource(R.string.mindrecord_more_menu_cd),
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .clickable(role = Role.Button) { menuExpanded = true },
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
                    title = "가족과 함께한 저녁 식사",
                    content = "오랜만에 가족들과 둘러앉아 이야기를 나누는 시간이 정말 좋았다.",
                    date = LocalDate.now(),
                    emotion = "😊",
                    imageUrl = "https://example.com/image.jpg",
                ),
            onClick = {},
            onDelete = {},
            onEdit = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryComponentNoImagePreview() {
    AfternoteTheme {
        DiaryComponent(
            diary =
                DailyDiary(
                    title = "프로젝트 완성",
                    content = "드디어 3개월간 준비한 프로젝트를 완성했다. 힘들었지만 뿌듯하다.",
                    date = LocalDate.now(),
                    emotion = "😊",
                ),
            onClick = {},
            onDelete = {},
            onEdit = {},
        )
    }
}
