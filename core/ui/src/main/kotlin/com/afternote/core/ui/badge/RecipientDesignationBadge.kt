package com.afternote.core.ui.badge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.modifierextention.shimmerLoadingPlaceholder
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 수신인 지정 칩의 유효한 상태만 표현한다.
 *
 * 레이아웃은 [CircularCheckboxOutlineChip]을 쓰고, 문구·색은 **수신인 지정** 도메인에만 맞춘다.
 *
 * - [Completed]: 지정 완료(상호작용 없음).
 * - [Incomplete]: 미완료. [Incomplete.onClick] 이 null 이 아니면 탭 가능(홈), null 이면 표시만(애프터노트 상세).
 */
sealed interface RecipientDesignationBadgeState {
    /**
     * 아직 조회되지 않았다 — 지정 여부를 **결과로 확정하지 않는다** (#698).
     *
     * 미결정을 `Incomplete` 로 그리면 이미 지정한 사용자도 진입할 때마다 «미완료» 배지를
     * 보고, 그 배지가 탭 가능한 CTA 라 하지 않아도 될 행동을 유도한다. 같은 화면의 오늘의
     * 질문·카테고리 카운트는 이미 미결정을 스켈레톤으로 표현하고 있어 표현도 어긋났다.
     */
    data object Unknown : RecipientDesignationBadgeState

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
        RecipientDesignationBadgeState.Unknown -> {
            // **콘텐츠를 그리지 않는다.** 칩에 shimmer 를 걸면 칩 내부의 불투명 배경이
            // shimmer 를 덮어 정지된 회색 칩이 되고, 라벨은 그 위에 그대로 그려진다 —
            // 「조회 전인데 확정된 무언가로 읽힘」이 문구만 바뀐 채 남는다.
            //
            // 같은 화면의 오늘의 질문·카테고리 카운트와 같은 형태로 자리만 잡고, 읽을
            // 것은 semantics 로 준다.
            val unknownDescription = stringResource(R.string.core_ui_recipient_designation_unknown)
            Box(
                modifier =
                    modifier
                        .width(UNKNOWN_BADGE_WIDTH)
                        .height(UNKNOWN_BADGE_HEIGHT)
                        .clip(RoundedCornerShape(20.dp))
                        .shimmerLoadingPlaceholder()
                        .semantics { contentDescription = unknownDescription },
            )
        }

        RecipientDesignationBadgeState.Completed -> {
            CircularCheckboxOutlineChip(
                label = stringResource(R.string.core_ui_recipient_designated),
                borderColor = AfternoteDesign.colors.gray2,
                backgroundColor = AfternoteDesign.colors.white,
                checkboxState = CheckboxState.Default,
                showTrailingArrow = false,
                onClick = null,
                modifier = modifier,
            )
        }

        is RecipientDesignationBadgeState.Incomplete -> {
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
}

/** 확정 상태 칩과 같은 자리를 차지하도록 맞춘 크기 (`CircularCheckboxOutlineChip` 실측). */
private val UNKNOWN_BADGE_WIDTH = 132.dp
private val UNKNOWN_BADGE_HEIGHT = 36.dp
