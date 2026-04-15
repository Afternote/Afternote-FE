package com.afternote.core.ui.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 원형 체크 아이콘 + 라벨 + (선택) 화살표를 담은 **외곽선 칩** 레이아웃.
 *
 * 문구·도메인 의미는 호출부가 [label]로 넘긴다. 수신인 지정·처리 방법 등 서로 다른 용도에서 재사용한다.
 */
@Composable
fun CircularCheckboxOutlineChip(
    label: String,
    modifier: Modifier = Modifier,
    borderColor: Color = AfternoteDesign.colors.gray2,
    backgroundColor: Color = AfternoteDesign.colors.white,
    checkboxState: CheckboxState = CheckboxState.Default,
    showTrailingArrow: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier =
            modifier
                .border(width = 1.dp, color = borderColor, shape = shape)
                .clip(shape)
                .background(backgroundColor)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AfternoteCircularCheckbox(
            state = checkboxState,
            size = 12.dp,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = AfternoteDesign.typography.captionLargeB,
            color = AfternoteDesign.colors.gray9,
        )
        if (showTrailingArrow) {
            Spacer(modifier = Modifier.width(10.dp))
            RightArrowIcon(
                modifier = Modifier.size(9.dp),
                tint = AfternoteDesign.colors.gray6,
            )
        }
    }
}
