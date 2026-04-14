package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.badge.RecipientDesignationBadge
import com.afternote.core.ui.badge.RecipientDesignationBadgeState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

/**
 * 애프터노트 작성자 상세에서 공통으로 쓰는 상단 블록.
 *
 * 시스템 인셋은 부모 스크롤 [Column] 의 [Spacer] 로 처리하고, 여기서는 서비스 아이콘·이름·최종 작성일 + [RecipientDesignationBadge] 만 담당한다.
 */
@Composable
fun AfternoteDetailServiceHeaderWithRecipientChip(
    iconResId: Int,
    serviceName: String,
    finalWriteDate: String,
    recipientBadgeState: RecipientDesignationBadgeState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .size(64.dp)
                    .border(1.dp, shape = CircleShape, color = AfternoteDesign.colors.gray2),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconResId),
                    contentDescription = serviceName,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = serviceName,
                    style = AfternoteDesign.typography.h2,
                    color = AfternoteDesign.colors.gray9,
                )
                Text(
                    text = stringResource(R.string.afternote_last_written_date, finalWriteDate),
                    style = AfternoteDesign.typography.inter,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        RecipientDesignationBadge(state = recipientBadgeState)
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteDetailServiceHeaderWithRecipientChipPreview() {
    AfternoteTheme {
        AfternoteDetailServiceHeaderWithRecipientChip(
            iconResId = R.drawable.feature_afternote_ic_memorial_guideline,
            serviceName = "서비스 이름",
            finalWriteDate = "2024.05.20",
            recipientBadgeState = RecipientDesignationBadgeState.Completed,
        )
    }
}
