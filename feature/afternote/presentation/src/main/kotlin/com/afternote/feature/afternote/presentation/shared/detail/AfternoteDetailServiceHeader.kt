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
import com.afternote.core.ui.badge.CircularCheckboxOutlineChip
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.AfternoteServiceDisplay

/**
 * 애프터노트 작성자 상세 공통 상단: 서비스 아이콘·이름·최종 작성일 + 처리 방법 칩.
 *
 * 하단 칩은 [CircularCheckboxOutlineChip]으로만 그리며, [processingMethodChipLabel]은 호출부(갤러리/소셜)에서
 * 작성·검증 단계에서 이미 선택된 `processing.method` 제목을 넘긴다. 수신인 지정([RecipientDesignationBadge])과 무관하다.
 */
@Composable
fun AfternoteDetailServiceHeader(
    service: AfternoteServiceDisplay,
    finalWriteDate: String,
    processingMethodChipLabel: String,
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
                    painter = painterResource(service.iconResId),
                    contentDescription = service.serviceName,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = service.serviceName,
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
        CircularCheckboxOutlineChip(
            label = processingMethodChipLabel,
            borderColor = AfternoteDesign.colors.gray2,
            backgroundColor = AfternoteDesign.colors.white,
            checkboxState = CheckboxState.Default,
            showTrailingArrow = false,
            onClick = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteDetailServiceHeaderPreview() {
    AfternoteTheme {
        AfternoteDetailServiceHeader(
            service =
                AfternoteServiceDisplay(
                    serviceName = "서비스 이름",
                    iconResId = R.drawable.feature_afternote_ic_memorial_guideline,
                ),
            finalWriteDate = "2024.05.20",
            processingMethodChipLabel = "가족 공유 앨범으로 이전",
        )
    }
}
