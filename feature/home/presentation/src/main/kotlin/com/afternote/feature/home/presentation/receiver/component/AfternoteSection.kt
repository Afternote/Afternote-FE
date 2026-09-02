package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.AfternoteSourceIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.home.presentation.R

private const val MAX_VISIBLE_ICONS = 4
private val ICON_SIZE = 32.dp

/**
 * 애프터노트 섹션 — 카운트 아래에 소셜 서비스 아이콘 행을 표시한다.
 *
 * [icons]가 4개를 넘으면 처음 4개만 보이고 마지막 자리에 «+N» 칩을 노출한다.
 */
@Composable
fun AfternoteSection(
    totalCount: Int?,
    icons: List<AfternoteSourceIcon>,
    onGoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeSectionCard(
        modifier = modifier,
        title = stringResource(R.string.home_receiver_afternote_section_title),
        description = stringResource(R.string.home_receiver_afternote_section_desc),
        countLine =
            rememberCountLine(
                prefix = stringResource(R.string.home_receiver_afternote_count_prefix, countText(totalCount)),
                suffix = stringResource(R.string.home_receiver_afternote_count_suffix),
            ),
        buttonText = stringResource(R.string.home_receiver_afternote_section_button),
        onButtonClick = onGoClick,
        middleContent = {
            if (totalCount == null) return@HomeSectionCard
            if (icons.isEmpty() && totalCount <= 0) return@HomeSectionCard
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteSourceIconRow(
                icons = icons,
                totalCount = totalCount,
            )
        },
    )
}

@Composable
private fun AfternoteSourceIconRow(
    icons: List<AfternoteSourceIcon>,
    totalCount: Int,
) {
    val visible = icons.take(MAX_VISIBLE_ICONS)
    val extra = (totalCount - visible.size).coerceAtLeast(0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visible.forEach { icon ->
            Image(
                painter = painterResource(icon.drawableResId),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(ICON_SIZE)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = AfternoteDesign.colors.gray2,
                            shape = CircleShape,
                        ),
                contentScale = ContentScale.Crop,
            )
        }
        if (extra > 0) {
            ExtraCountChip(count = extra)
        }
    }
}

@Composable
private fun ExtraCountChip(count: Int) {
    Box(
        modifier =
            Modifier
                .size(ICON_SIZE)
                .clip(CircleShape)
                .background(AfternoteDesign.colors.gray9),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_receiver_afternote_extra_count, count),
            style = AfternoteDesign.typography.captionLargeB,
            color = AfternoteDesign.colors.white,
        )
    }
}
