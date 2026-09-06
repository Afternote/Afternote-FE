package com.afternote.core.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import androidx.compose.ui.window.Popup as ComposePopup

/** 액션 메뉴 한 줄. [label] 은 이미 해석된 문구다 — 호출부가 리소스를 고를 수 있게 String 으로 받는다. */
data class ActionMenuItem(
    val label: String,
    val onClick: () -> Unit,
)

/** 시안 확정값 (#643): 흰 카드 · 8dp 라운드 · 8dp 그림자 · 항목 패딩 16dp 전방향. */
private val ActionMenuShape = RoundedCornerShape(8.dp)
private val ActionMenuShadowElevation = 8.dp
private val ActionMenuItemPadding = 16.dp
private const val ACTION_MENU_SHADOW_ALPHA = 0.15f

/**
 * 「더보기(⋯)」 앵커에서 펼쳐지는 공용 액션 팝업 메뉴 (#643).
 *
 * afternote·mindrecord·timeletter 에 4벌로 흩어져 있던 «수정/삭제» 팝업의 정본이다.
 * 시각 계약은 이 컴포저블이 쥐고, **등장 위치는 [popupPositionProvider] 로만** 결정한다 —
 * 기본값은 창 우측 15dp · 앵커 아래 12dp ([rememberFixedRightPopupPositionProvider]).
 *
 * Material3 [androidx.compose.material3.DropdownMenu] 는 그림자·항목 최소 높이(48dp)·컨테이너
 * 색이 가이드라인 값으로 강제돼 있어 시안(흰 배경·8dp 라운드)을 맞추려면 우회가 지저분해진다.
 * 그래서 `androidx.compose.ui.window.Popup` 위에 직접 쌓는다. 대신 등장 페이드·스케일
 * 애니메이션은 없다.
 *
 * **dismiss 계약**: 항목을 누르면 [onDismissRequest] 를 **먼저** 부르고 그다음
 * [ActionMenuItem.onClick] 을 부른다. 항목 콜백이 다이얼로그를 띄우는 흔한 경우에
 * 메뉴가 뒤에 남는 것을 호출부마다 막지 않아도 되게 한다.
 *
 * @param items 위에서부터 그릴 순서 그대로. **비면 호출부 오류다** (`require`) —
 *   「항목을 숨긴다」는 판단은 호출부가 리스트에서 빼는 것으로 표현하되, 열 항목이 하나도
 *   없는 메뉴를 여는 호출은 있어서는 안 된다. 너비는 가장 긴 항목(`IntrinsicSize.Max`)에 맞춘다.
 */
@Composable
fun AfternoteActionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<ActionMenuItem>,
    modifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = rememberFixedRightPopupPositionProvider(),
) {
    require(items.isNotEmpty()) { "AfternoteActionMenu 는 항목이 하나 이상 있어야 한다" }
    if (!expanded) return

    ComposePopup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        AfternoteActionMenuCard(
            items =
                items.map { item ->
                    ActionMenuItem(label = item.label) {
                        onDismissRequest()
                        item.onClick()
                    }
                },
            modifier = modifier,
        )
    }
}

/** 팝업 내부 카드의 그림자·둥글기·배경·항목 배치를 구현한다. */
@Composable
private fun AfternoteActionMenuCard(
    items: List<ActionMenuItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .shadow(
                    elevation = ActionMenuShadowElevation,
                    shape = ActionMenuShape,
                    clip = true,
                    spotColor = AfternoteDesign.colors.black.copy(alpha = ACTION_MENU_SHADOW_ALPHA),
                ).background(AfternoteDesign.colors.white)
                .width(IntrinsicSize.Max),
    ) {
        items.forEach { item ->
            ActionMenuRow(item = item, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ActionMenuRow(
    item: ActionMenuItem,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clickable(role = Role.Button, onClick = item.onClick)
                .padding(ActionMenuItemPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = item.label,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

/**
 * 「수정하기 → 삭제하기」 표준 쌍 (#643 확정 순서).
 *
 * 문구가 core 리소스라 호출 모듈이 `core.ui.R` 을 직접 import 하지 않아도 된다.
 *
 * @param onEditClick **null 이면 수정 항목 자체를 그리지 않는다** — 편집이 없는 메뉴(수신자 행)와
 *   「핸들러를 안 넘겨 죽은 버튼이 그려지는」 미배선을 타입으로 구분하기 위해 플래그 + no-op
 *   디폴트 대신 nullable 핸들러 하나로 모델링한다 (#1388).
 */
@Composable
fun editDeleteActionMenuItems(
    onEditClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
): List<ActionMenuItem> {
    val editLabel = stringResource(R.string.core_ui_action_menu_edit)
    val deleteLabel = stringResource(R.string.core_ui_action_menu_delete)
    return buildList {
        if (onEditClick != null) {
            add(ActionMenuItem(label = editLabel, onClick = onEditClick))
        }
        add(ActionMenuItem(label = deleteLabel, onClick = onDeleteClick))
    }
}
