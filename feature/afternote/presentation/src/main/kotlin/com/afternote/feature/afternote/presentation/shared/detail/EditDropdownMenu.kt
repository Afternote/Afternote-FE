package com.afternote.feature.afternote.presentation.shared.detail

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.afternote.core.ui.popup.rememberFixedRightPopupPositionProvider
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import androidx.compose.ui.window.Popup as ComposePopup

/**
 * 상세 화면 우측 상단 "더보기" 버튼에서 펼쳐지는 커스텀 드롭다운.
 *
 * Material3 [androidx.compose.material3.DropdownMenu] 는 그림자/애니메이션 시작점/
 * 아이템 최소 높이(48dp) 등이 가이드라인에 맞게 강제돼 있어 우회 시 코드가 지저분해진다.
 * 둥글기·폰트·그림자·패딩은 이 컴포저블이 담당하고, 등장 위치는 [popupPositionProvider] 로만 결정한다.
 * 기본값은 창 우측 15dp · 앵커 아래 12dp ([rememberFixedRightPopupPositionProvider]).
 *
 * Material3 `DropdownMenu` 과 달리 `androidx.compose.ui.window.Popup` 기반이라
 * 등장 페이드·스케일 애니메이션은 없다. 필요하면 메뉴 `Column` 을
 * `androidx.compose.animation.AnimatedVisibility` 로 감싸면 된다.
 *
 * @param onEditClick 편집 항목 클릭 콜백. **null 이면 편집 항목 자체를 그리지 않는다** — 편집이
 *   없는 메뉴(수신자 행)와 "핸들러를 안 넘겨 죽은 편집 버튼이 그려지는" 미배선을 타입으로 구분하기
 *   위해 `showEditItem` 플래그 + no-op 디폴트 대신 nullable 핸들러 하나로 모델링한다 (#1388).
 */
@Composable
fun EditDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = rememberFixedRightPopupPositionProvider(),
) {
    if (!expanded) return

    ComposePopup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        EditDropdownMenuItems(
            onEditClick =
                onEditClick?.let { onEdit ->
                    {
                        onDismissRequest()
                        onEdit()
                    }
                },
            onDeleteClick = {
                onDismissRequest()
                onDeleteClick()
            },
            modifier = modifier,
        )
    }
}

/**
 * 펼친 항목 리스트(그림자·둥글기·배경 포함) — Popup 컨텍스트 밖에서도 그려져 Preview 단독 확인 가능.
 *
 * 자식 클릭 영역을 가장 긴 항목 너비로 통일하기 위해 `IntrinsicSize.Max` + `fillMaxWidth`를 사용한다.
 */
@Composable
private fun EditDropdownMenuItems(
    onEditClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(8.dp),
                    clip = true,
                    spotColor = AfternoteDesign.colors.black.copy(alpha = 0.15f),
                ).background(AfternoteDesign.colors.white)
                .width(IntrinsicSize.Max),
    ) {
        if (onEditClick != null) {
            CustomDropdownItem(
                text = stringResource(R.string.afternote_menu_edit),
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        CustomDropdownItem(
            text = stringResource(R.string.afternote_menu_delete_record),
            onClick = onDeleteClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CustomDropdownItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 30.5.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}
