package com.afternote.core.ui.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 수신인 지정 상태를 표시하는 공용 뱃지.
 *
 * 홈 탭의 수신인 지정 칩과 애프터노트 상세의 "수신인 지정 완료" 뱃지가 공유하는 레이아웃을 제공한다.
 * 기본값은 상세 화면 뱃지 스타일에 맞춰져 있으며, 홈 탭 등에서 배경/패딩/타이포/후행 요소를 주입해 변형한다.
 *
 * @param text 표시할 라벨
 * @param checkboxState 좌측 원형 체크박스의 상태 (표시 전용, 내부 onClick 은 항상 null)
 * @param onClick null 이 아니면 전체 영역이 클릭 가능한 칩으로 동작
 * @param trailingContent 텍스트 뒤에 배치할 요소. 자체 leading spacing 을 포함해야 한다.
 */
@Composable
fun RecipientDesignationBadge(
    text: String,
    modifier: Modifier = Modifier,
    checkboxState: CheckboxState = CheckboxState.Default,
    checkboxSize: Dp = 20.dp,
    textStyle: TextStyle = AfternoteDesign.typography.captionLargeR,
    textColor: Color = AfternoteDesign.colors.gray7,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = AfternoteDesign.colors.gray3,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    horizontalSpacing: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
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
                ).padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AfternoteCircularCheckbox(
            state = checkboxState,
            onClick = null,
            size = checkboxSize,
        )
        Spacer(modifier = Modifier.width(horizontalSpacing))
        Text(
            text = text,
            style = textStyle,
            color = textColor,
        )
        trailingContent?.invoke(this)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RecipientDesignationBadgeDefaultPreview() {
    AfternoteTheme {
        RecipientDesignationBadge(text = "수신인 지정 완료")
    }
}
