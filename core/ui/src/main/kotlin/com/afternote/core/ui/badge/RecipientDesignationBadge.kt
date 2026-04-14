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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 수신인 지정 완료/미완료 상태를 한 가지 칩 UI로 표시한다.
 *
 * 외부에서는 [isCompleted] 만 넘기고, 문구·색·체크·테두리·화살표·클릭 가능 여부는 모두 내부에서 결정한다.
 * - [onClick] 은 **미완료**이고 null 이 아닐 때만 적용한다. 완료 상태에서는 무시된다.
 * - 미완료 시 화살표는 항상 표시한다(홈·애프터노트 상세 동일). 클릭만 [onClick] 유무로 갈린다.
 *   (애프터노트 상세의 전달/지정 완료 등 구분).
 */
@Composable
fun RecipientDesignationBadge(
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier =
            modifier
                .border(
                    width = 1.dp,
                    color =
                        if (isCompleted) {
                            AfternoteDesign.colors.gray2
                        } else {
                            AfternoteDesign.colors.gray3
                        },
                    shape = shape,
                ).clip(shape)
                .background(
                    if (isCompleted) {
                        AfternoteDesign.colors.white
                    } else {
                        AfternoteDesign.colors.gray2
                    },
                ).then(
                    if (!isCompleted && onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AfternoteCircularCheckbox(
            state =
                if (isCompleted) {
                    CheckboxState.Default
                } else {
                    CheckboxState.None
                },
            onClick = null,
            size = 12.dp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text =
                stringResource(
                    if (isCompleted) {
                        R.string.core_ui_recipient_designated
                    } else {
                        R.string.core_ui_recipient_not_designated
                    },
                ),
            style = AfternoteDesign.typography.captionLargeB,
            color = AfternoteDesign.colors.gray9,
        )
        if (!isCompleted) {
            Spacer(modifier = Modifier.width(10.dp))
            RightArrowIcon(
                modifier = Modifier.size(9.dp),
                tint = AfternoteDesign.colors.gray6,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "수신인 지정 완료")
@Composable
private fun RecipientDesignationBadgeCompletePreview() {
    AfternoteTheme {
        RecipientDesignationBadge(isCompleted = true)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "수신인 지정 미완료(클릭)")
@Composable
private fun RecipientDesignationBadgeIncompletePreview() {
    AfternoteTheme {
        RecipientDesignationBadge(
            isCompleted = false,
            onClick = {},
        )
    }
}
