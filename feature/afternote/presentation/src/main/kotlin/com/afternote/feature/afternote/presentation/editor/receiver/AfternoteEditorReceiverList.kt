package com.afternote.feature.afternote.presentation.editor.receiver

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.popup.AfternoteActionMenu
import com.afternote.core.ui.popup.editDeleteActionMenuItems
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.shared.ReceiverAvatar

/**
 * 수신자 리스트 컴포넌트
 *
 * 피그마 디자인 기반:
 * - 흰색 배경, 둥근 모서리 16dp
 * - 수신자 아이템 리스트
 * - 하단 중앙에 추가 버튼 (파란 원형 버튼)
 */
@Composable
fun AfternoteEditorReceiverList(
    modifier: Modifier = Modifier,
    afternoteEditReceivers: List<AfternoteEditorReceiver>,
    onAddClick: () -> Unit,
    onItemDeleteClick: (Long) -> Unit,
    state: AfternoteEditorReceiverListState = rememberAfternoteEditorReceiverListState(),
) {
    val focusManager = LocalFocusManager.current

    // 초기화: 수신자들의 expanded 상태 설정
    LaunchedEffect(afternoteEditReceivers) {
        state.initializeExpandedStates(afternoteEditReceivers, null)
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = AfternoteDesign.colors.white, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        afternoteEditReceivers.forEachIndexed { _, receiver ->
            AfternoteEditorReceiverItem(
                receiver = receiver,
                expanded = state.expandedStates[receiver.id] ?: false,
                onMoreClick = {
                    focusManager.clearFocus()
                    state.toggleItemExpanded(receiver.id)
                },
                onDismissDropdown = {
                    state.expandedStates[receiver.id] = false
                },
                onDeleteClick = { onItemDeleteClick(receiver.id) },
            )
        }

        PlusBadgeButton(
            contentDescription = stringResource(R.string.afternote_editor_content_description_add),
            onClick = {
                state.toggleTextField()
                onAddClick()
            },
        )
    }
}

/**
 * 수신자 아이템 컴포넌트
 *
 * 피그마 디자인 기반:
 * - 아바타: 회색 원형 배경, 40dp
 * - 이름: 14sp, Regular, AfternoteDesign.colors.gray9
 * - 라벨: 12sp, Regular, AfternoteDesign.colors.gray5
 * - 더보기 아이콘: 오른쪽 정렬
 */
@Composable
private fun AfternoteEditorReceiverItem(
    modifier: Modifier = Modifier,
    receiver: AfternoteEditorReceiver,
    expanded: Boolean = false,
    onMoreClick: () -> Unit,
    onDismissDropdown: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReceiverAvatar()
        Spacer(Modifier.width(10.dp))
        // 이름과 라벨
        Column {
            Text(
                text = receiver.name,
                style =
                    AfternoteDesign.typography.captionLargeB.copy(
                        color = AfternoteDesign.colors.gray9,
                    ),
            )
            Text(
                text = receiver.label,
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        color = AfternoteDesign.colors.gray8,
                    ),
            )
        }
        Spacer(Modifier.weight(1f))
        // 더보기 아이콘 + 드롭다운 메뉴
        Box {
            Image(
                painter = painterResource(R.drawable.afternote_ic_more_horizontal_1),
                contentDescription = stringResource(R.string.afternote_editor_content_description_more),
                modifier =
                    Modifier
                        .clickable(role = Role.Button, onClick = onMoreClick),
            )
            AfternoteActionMenu(
                expanded = expanded,
                onDismissRequest = onDismissDropdown,
                items =
                    editDeleteActionMenuItems(
                        // 수신자 행 메뉴엔 편집이 없다 — null 이 수정 항목 자체를 리스트에서 뺀다.
                        onEditClick = null,
                        onDeleteClick = onDeleteClick,
                    ),
            )
        }
    }
}
