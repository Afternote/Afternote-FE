package com.afternote.feature.mindrecord.presentation.component.hometab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.modifierextention.shimmerLoadingPlaceholder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_total_label
import java.text.NumberFormat
import java.util.Locale
import com.afternote.core.ui.R as CoreUiR

private val CardShape = RoundedCornerShape(6.dp)

@Composable
fun RecordCategoryCard(
    iconResId: Int,
    title: String,
    subtitle: String,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // TODO: Vector Asset(xml)의 viewport 여백을 맞추면 이 플래그를 제거할 수 있습니다.
    useDiaryIconLayout: Boolean = false,
    isCountLoading: Boolean = false,
) {
    val formattedCount =
        remember(totalCount) {
            NumberFormat.getNumberInstance(Locale.getDefault()).format(totalCount)
        }

    Column(
        modifier =
            modifier
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray2,
                    shape = CardShape,
                ).clip(CardShape)
                .background(AfternoteDesign.colors.white)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(
                            color = AfternoteDesign.colors.gray2,
                            shape = CircleShape,
                        ),
                contentAlignment = if (useDiaryIconLayout) Alignment.TopStart else Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = AfternoteDesign.colors.gray6,
                    modifier =
                        if (useDiaryIconLayout) {
                            Modifier
                                .padding(
                                    start = 9.dp,
                                    top = 5.dp,
                                    end = 7.dp,
                                    bottom = 8.dp,
                                ).size(width = 17.dp, height = 19.dp)
                        } else {
                            Modifier.size(20.dp)
                        },
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray5,
            lineHeight = 18.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(mindrecord_total_label),
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray5,
            )
            if (isCountLoading) {
                Box(
                    modifier =
                        Modifier
                            .width(40.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerLoadingPlaceholder(),
                )
            } else {
                Text(
                    text = formattedCount,
                    style = AfternoteDesign.typography.h3,
                    color = AfternoteDesign.colors.gray9,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordCategoryCardPreview() {
    AfternoteTheme {
        RecordCategoryCard(
            modifier = Modifier.padding(16.dp),
            iconResId = CoreUiR.drawable.core_ui_ic_diary,
            title = "일기",
            subtitle = "나의 매일을 기록하세요",
            totalCount = 18,
            onClick = {},
            useDiaryIconLayout = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordCategoryCardLargeCountPreview() {
    AfternoteTheme {
        RecordCategoryCard(
            modifier = Modifier.padding(16.dp),
            iconResId = CoreUiR.drawable.core_ui_ic_deep_thought,
            title = "깊은 생각",
            subtitle = "오늘의 생각을 남기세요",
            totalCount = 12_345,
            onClick = {},
        )
    }
}
