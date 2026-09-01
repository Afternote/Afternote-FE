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
import androidx.compose.ui.semantics.Role
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
 *
 * [enabled] 가 false 면 클릭이 차단될 뿐 아니라 배경이 흰색→gray2, 보더가 gray2→gray3 로 바뀌어
 * 비활성 상태가 시각적으로 드러난다 (비활성 버튼 `AfternoteButtonType.Un` 과 동일 팔레트). 내부 [content]
 * 의 색은 슬롯 주입이라 호출부가 상태에 맞춰 결정한다.
 *
 * [isError] 는 보더만 error 색으로 바꾼다(시안 `3628:23437` 로그인 자격 거절 상태). 비활성이
 * 우선한다 — 입력이 막힌 필드에 에러 강조가 남는 조합을 시안이 정의하지 않아서다.
 *
 * 폭 / 크기 정책은 호출부가 [modifier] 로 결정 (Compose API 가이드라인: element function 의 modifier
 * default 는 빈 `Modifier`). 부모 폭 차지가 필요하면 `Modifier.fillMaxWidth()`, Row 안에서
 * 가변 비율이면 `Modifier.weight(...)` 등을 명시적으로 넘긴다.
 */
@Composable
fun AfternoteFieldContainer(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val backgroundColor = if (enabled) AfternoteDesign.colors.white else AfternoteDesign.colors.gray2
    val borderColor =
        when {
            !enabled -> AfternoteDesign.colors.gray3
            isError -> AfternoteDesign.colors.error
            else -> AfternoteDesign.colors.gray2
        }
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = verticalAlignment,
        content = content,
    )
}
