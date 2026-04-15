package com.afternote.core.ui.badge

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.R
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 수신인 지정 칩의 유효한 상태만 표현한다.
 *
 * 레이아웃은 [CircularCheckboxOutlineChip]을 쓰고, 문구·색은 **수신인 지정** 도메인에만 맞춘다.
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

/**
 * 홈 등 **수신인 지정** 전용 칩. 처리 방법 등 다른 문구는 [CircularCheckboxOutlineChip]을 직접 쓴다.
 */
@Composable
fun RecipientDesignationBadge(
    state: RecipientDesignationBadgeState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        RecipientDesignationBadgeState.Completed ->
            CircularCheckboxOutlineChip(
                label = stringResource(R.string.core_ui_recipient_designated),
                borderColor = AfternoteDesign.colors.gray2,
                backgroundColor = AfternoteDesign.colors.white,
                checkboxState = CheckboxState.Default,
                showTrailingArrow = false,
                onClick = null,
                modifier = modifier,
            )
        is RecipientDesignationBadgeState.Incomplete ->
            CircularCheckboxOutlineChip(
                label = stringResource(R.string.core_ui_recipient_not_designated),
                borderColor = AfternoteDesign.colors.gray3,
                backgroundColor = AfternoteDesign.colors.gray2,
                checkboxState = CheckboxState.None,
                showTrailingArrow = true,
                onClick = state.onClick,
                modifier = modifier,
            )
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
