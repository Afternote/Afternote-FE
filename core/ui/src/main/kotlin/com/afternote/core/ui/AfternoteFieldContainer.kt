package com.afternote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 입력 필드 / 슬롯 카드 등 디자인 시스템의 시각 컨테이너 단위.
 *
 * `AfternoteTextField` 내부 `TextFieldShort` 의 decorator 영역과 수신자 인증의
 * `DocumentSlotCard` 가 동일한 외곽(흰 배경 + gray2 1dp 보더 + 8dp 코너 + 24/13 padding)
 * 을 별도로 박아두던 것을 한 곳으로 모은다.
 *
 * [onClick] 을 주면 ripple 이 둥근 코너 안쪽으로 잘리도록 `clip` 뒤에 `clickable` 이 걸린다.
 */
@Composable
fun AfternoteFieldContainer(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AfternoteDesign.colors.white)
                .border(1.dp, AfternoteDesign.colors.gray2, RoundedCornerShape(8.dp))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(enabled = enabled, onClick = onClick)
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = verticalAlignment,
        content = content,
    )
}
