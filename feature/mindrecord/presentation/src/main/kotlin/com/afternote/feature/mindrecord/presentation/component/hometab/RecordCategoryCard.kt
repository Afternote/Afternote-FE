package com.afternote.feature.mindrecord.presentation.component.hometab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_record_category_accessibility
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_total_label
import java.text.NumberFormat
import java.util.Locale

/** 글꼴 확대·좁은 가로에서도 카드가 과도하게 눌리지 않도록 쓰는 최소 높이. */
private val RecordCategoryCardMinHeight = 136.dp

@Composable
fun RecordCategoryCard(
    iconResId: Int,
    title: String,
    subtitle: String,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedCount =
        remember(totalCount) {
            NumberFormat.getNumberInstance(Locale.getDefault()).format(totalCount)
        }
    val categoryContentDescription =
        stringResource(mindrecord_record_category_accessibility, title, formattedCount)

    Surface(
        modifier =
            modifier
                .heightIn(min = RecordCategoryCardMinHeight)
                .semantics(mergeDescendants = true) {
                    contentDescription = categoryContentDescription
                },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
        color = AfternoteDesign.colors.white,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = AfternoteDesign.colors.gray1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            tint = AfternoteDesign.colors.gray7,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = AfternoteDesign.typography.bodyLargeB,
                    color = AfternoteDesign.colors.black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = subtitle,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray5,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(mindrecord_total_label),
                    style = AfternoteDesign.typography.mono,
                    color = AfternoteDesign.colors.gray5,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = formattedCount,
                    style = AfternoteDesign.typography.h1,
                    color = AfternoteDesign.colors.black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun Modifier.recordCategoryCardPreviewSizing(): Modifier =
    this
        .width(160.dp)
        .heightIn(min = RecordCategoryCardMinHeight)
        .aspectRatio(1.1f)

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "기본")
@Composable
private fun RecordCategoryCardPreview() {
    AfternoteTheme {
        RecordCategoryCard(
            modifier = Modifier.recordCategoryCardPreviewSizing(),
            iconResId = android.R.drawable.ic_menu_edit,
            title = "일기",
            subtitle = "나의 매일을 기록하세요",
            totalCount = 18,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "큰 수")
@Composable
private fun RecordCategoryCardLargeCountPreview() {
    AfternoteTheme {
        RecordCategoryCard(
            modifier = Modifier.recordCategoryCardPreviewSizing(),
            iconResId = android.R.drawable.ic_menu_edit,
            title = "깊은 생각",
            subtitle = "오늘의 생각을 남기세요",
            totalCount = 12_345,
            onClick = {},
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    fontScale = 1.5f,
    name = "글꼴 확대",
)
@Composable
private fun RecordCategoryCardFontScalePreview() {
    AfternoteTheme {
        RecordCategoryCard(
            modifier = Modifier.recordCategoryCardPreviewSizing(),
            iconResId = android.R.drawable.ic_menu_edit,
            title = "일기",
            subtitle = "나의 매일을 기록하세요. 글꼴이 커져도 레이아웃이 무너지지 않는지 확인합니다.",
            totalCount = 18,
            onClick = {},
        )
    }
}
