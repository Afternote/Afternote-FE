package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.afternote.core.ui.popup.rememberFixedRightPopupPositionProvider
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
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
 */
@Composable
fun EditDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    showEditItem: Boolean = true,
    popupPositionProvider: PopupPositionProvider = rememberFixedRightPopupPositionProvider(),
) {
    if (!expanded) return

    ComposePopup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier =
                modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp),
                        clip = true,
                        spotColor = AfternoteDesign.colors.black.copy(alpha = 0.15f),
                    ).background(AfternoteDesign.colors.white),
        ) {
            if (showEditItem) {
                CustomDropdownItem(
                    text = stringResource(R.string.feature_afternote_menu_edit),
                    onClick = {
                        onDismissRequest()
                        onEditClick()
                    },
                )
            }
            CustomDropdownItem(
                text = stringResource(R.string.feature_afternote_menu_delete_record),
                onClick = {
                    onDismissRequest()
                    onDeleteClick()
                },
            )
        }
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
                .semantics { role = Role.Button }
                .clickable(onClick = onClick)
                .padding(horizontal = 26.dp, vertical = 16.dp),
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300)
@Composable
private fun EditDropdownMenuPreview() {
    AfternoteTheme {
        EditDropdownMenu(
            expanded = true,
            onDismissRequest = {},
            onDeleteClick = {},
            onEditClick = {},
            showEditItem = true,
            popupPositionProvider =
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset = IntOffset(x = 50, y = 50)
                },
        )
    }
}
