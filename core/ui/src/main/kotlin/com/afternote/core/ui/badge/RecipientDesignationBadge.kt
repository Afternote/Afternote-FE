package com.afternote.core.ui.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun RecipientDesignationBadge(
    text: String, // 수신인 지정 완료, 수신인 지정 미완료 두 가지만 존재
    modifier: Modifier = Modifier,
    checkboxState: CheckboxState = CheckboxState.Default, // 디폴트, 논 두 가지만 존재
    backgroundColor: Color = Color.Transparent, // 화이트, 그레이2만 존제
    borderColor: Color = AfternoteDesign.colors.gray3, // 그레이2, 그레이3만 존재
    onClick: (() -> Unit)? = null, // 수신인 지정 미완료 시에만 존재
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    // 수신인 지정 미완료 시에만 존재하며 Spacer(modifier = Modifier.width(10.dp)) RightArrowIcon( modifier = Modifier.size(12.dp), tint = AfternoteDesign.colors.gray5,)만 옴
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
            onClick = null,
            size = 12.dp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = AfternoteDesign.typography.captionLargeB,
            color = AfternoteDesign.colors.gray9,
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
