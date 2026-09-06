package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
 * Figma 2671:16732 — 일기 카드 형 그리드 카드: 이미지(있을 때, 이모지 배지 오버레이) + 날짜·요일 + 제목 + 내용.
 */
@Composable
fun DiaryCard(
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
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column {
            if (diary.imageUrl != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = diary.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(AfternoteDesign.colors.gray1),
                    )

                    diary.emotion?.let {
                        Text(
                            text = it,
                            modifier =
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                        )
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = diary.date.format(DateFormatter),
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = AfternoteDesign.colors.gray6,
                    )
                    Text(
                        text = diary.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = AfternoteDesign.colors.gray6,
                    )
                    if (diary.imageUrl == null) {
                        diary.emotion?.let { Text(text = it) }
                    }
                }

                Box {
                    Icon(
                        painter = painterResource(R.drawable.mindrecord_horizontal),
                        tint = AfternoteDesign.colors.gray6,
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

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = diary.title,
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray9,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = diary.content.htmlToPlainText(),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
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
private fun DiaryCardNoImagePreview() {
    AfternoteTheme {
        DiaryCard(
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
