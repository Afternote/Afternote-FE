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
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.util.htmlToPlainText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CardShape = RoundedCornerShape(6.dp)
private val DateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

@Composable
fun DailyQuestionListCard(
    answer: DailyQuestion,
    modifier: Modifier = Modifier,
    /**
     * 카드 전체 탭 — 저장된 기록 본문을 여는 상세 화면으로 간다 (#759).
     *
     * **`null` 이면 클릭 자체를 붙이지 않는다** (#1540 리뷰).
     *
     * no-op 을 넘기면 `Role.Button` 이 그대로 실려 스크린리더가 「버튼」으로 읽는데 눌러도
     * 아무 일이 없다 — 읽기 전용으로 쓰는 자리(주간 리포트 HISTORY)가 그랬다.
     */
    onClick: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = AfternoteDesign.colors.white,
            ),
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        shape = CardShape,
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        if (answer.imageUrl != null) {
            ThumbnailCardContent(answer = answer, onEdit = onEdit, onDelete = onDelete)
        } else {
            TextCardContent(answer = answer, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

// Figma 2757:16130 — 텍스트만 있는 카드: p=16 / 날짜·요일 → 제목(gap 8) → 내용(gap 4)
@Composable
private fun TextCardContent(
    answer: DailyQuestion,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.padding(16.dp),
    ) {
        CardHeaderRow(date = answer.date, onEdit = onEdit, onDelete = onDelete)

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = answer.title,
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray9,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer.content.htmlToPlainText(),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
    }
}

// Figma 2757:16129 — 썸네일 카드: p=12 / 85dp 이미지 + (날짜·요일 → 제목 → 내용 2줄)
@Composable
private fun ThumbnailCardContent(
    answer: DailyQuestion,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = answer.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(85.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(AfternoteDesign.colors.gray1),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CardHeaderRow(date = answer.date, onEdit = onEdit, onDelete = onDelete)
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
}

@Composable
private fun CardHeaderRow(
    date: LocalDate,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = date.format(DateFormatter),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )
        }

        // **핸들러가 없으면 «더보기» 자체를 그리지 않는다** (#1540).
        //
        // 종전에는 `= {}` 디폴트라, 핸들러를 안 넘긴 화면(주간 리포트 HISTORY)에서도 메뉴가
        // 뜨고 «수정»·«삭제» 를 눌러도 아무 일이 없었다. 눌러도 아무 일 없는 버튼을 그리는
        // 대신 없는 상호작용은 보여 주지 않는다.
        if (onEdit != null && onDelete != null) {
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
    }
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
            onClick = {},
            onDelete = {},
            onEdit = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionCardWithImagePreview() {
    AfternoteTheme {
        DailyQuestionListCard(
            answer =
                DailyQuestion(
                    title = "채연아 20번째 생일을 축하해",
                    content = "너가 태어난 게 엊그제같은데 벌써 스무살이라니.. 엄마가 없어도 씩씩하게 컸을 채연이를 상상하면 너무 기특해서 안아주고 싶구나",
                    date = LocalDate.now(),
                    imageUrl = "https://example.com/image.jpg",
                ),
            onClick = {},
            onDelete = {},
            onEdit = {},
        )
    }
}
