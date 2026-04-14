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
 * 수신인 지정 칩의 유효한 상태만 표현한다.
 *
 * - [Completed]: 지정 완료(상호작용 없음).
 * - [Incomplete]: 미완료. [Incomplete.onClick] 이 null 이 아니면 탭 가능(홈), null 이면 표시만(애프터노트 상세).
 */
sealed interface RecipientDesignationBadgeState {
    data object Completed : RecipientDesignationBadgeState

    data class Incomplete(
        val onClick: (() -> Unit)? = null,
    ) : RecipientDesignationBadgeState
}

private data class RecipientDesignationChipStyle(
    val labelRes: Int,
    val borderColor: Color,
    val backgroundColor: Color,
    val checkboxState: CheckboxState,
    val showTrailingArrow: Boolean,
)

@Composable
private fun chipStyleFor(state: RecipientDesignationBadgeState): RecipientDesignationChipStyle =
    when (state) {
        RecipientDesignationBadgeState.Completed ->
            RecipientDesignationChipStyle(
                labelRes = R.string.core_ui_recipient_designated,
                borderColor = AfternoteDesign.colors.gray2,
                backgroundColor = AfternoteDesign.colors.white,
                checkboxState = CheckboxState.Default,
                showTrailingArrow = false,
            )
        is RecipientDesignationBadgeState.Incomplete ->
            RecipientDesignationChipStyle(
                labelRes = R.string.core_ui_recipient_not_designated,
                borderColor = AfternoteDesign.colors.gray3,
                backgroundColor = AfternoteDesign.colors.gray2,
                checkboxState = CheckboxState.None,
                showTrailingArrow = true,
            )
    }

/**
 * 수신인 지정 완료/미완료 상태를 한 가지 칩 UI로 표시한다.
 *
 * [RecipientDesignationBadgeState] 로 완료 상태에 클릭을 붙이는 식의 조합을 막는다.
 * 색·체크·문구·화살표는 [chipStyleFor] 의 exhaustive `when` 으로 [state] 에 매핑한다.
 */
@Composable
fun RecipientDesignationBadge(
    state: RecipientDesignationBadgeState,
    modifier: Modifier = Modifier,
) {
    val style = chipStyleFor(state)
    val clickHandler =
        when (state) {
            is RecipientDesignationBadgeState.Incomplete -> state.onClick
            else -> null
        }

    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier =
            modifier
                .border(width = 1.dp, color = style.borderColor, shape = shape)
                .clip(shape)
                .background(style.backgroundColor)
                .then(
                    if (clickHandler != null) {
                        Modifier.clickable(role = Role.Button, onClick = clickHandler)
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AfternoteCircularCheckbox(
            state = style.checkboxState,
            onClick = null,
            size = 12.dp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(style.labelRes),
            style = AfternoteDesign.typography.captionLargeB,
            color = AfternoteDesign.colors.gray9,
        )
        if (style.showTrailingArrow) {
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
        RecipientDesignationBadge(state = RecipientDesignationBadgeState.Completed)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "수신인 지정 미완료")
@Composable
private fun RecipientDesignationBadgeIncompletePreview() {
    AfternoteTheme {
        RecipientDesignationBadge(
            state = RecipientDesignationBadgeState.Incomplete(onClick = {}),
        )
    }
}
