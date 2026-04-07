package com.afternote.feature.afternote.presentation.author.editor.receiver

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AddCircleButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverCallbacks
import com.afternote.feature.afternote.presentation.author.editor.provider.FakeAfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.shared.DataProviderLocals
import com.afternote.feature.afternote.presentation.shared.detail.EditDropdownMenu

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
    events: AfternoteEditorReceiverCallbacks = AfternoteEditorReceiverCallbacks(),
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
                showEditItem = false,
                onDeleteClick = { events.onItemDeleteClick(receiver.id) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 추가 버튼 (파란 원형 버튼)
        AddCircleButton(
            contentDescription = "수신자 추가",
            onClick = {
                state.toggleTextField()
                events.onAddClick()
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
    onMoreClick: () -> Unit = {},
    onDismissDropdown: () -> Unit = {},
    showEditItem: Boolean = true,
    onDeleteClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 아바타 (기본 프로필 이미지)
        Image(
            painter = painterResource(R.drawable.img_recipient_profile),
            contentDescription = "프로필 사진",
            modifier = Modifier.size(58.dp),
        )

        // 이름과 라벨
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = receiver.name,
                style =
                    AfternoteDesign.typography.bodySmallR.copy(
                        fontWeight = FontWeight.Medium,
                        color = AfternoteDesign.colors.gray9,
                    ),
            )
            Text(
                text = receiver.label,
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        color = AfternoteDesign.colors.gray5,
                    ),
            )
        }

        // 더보기 아이콘 + 드롭다운 메뉴
        Box {
            Image(
                painter = painterResource(R.drawable.ic_more_horizontal_1),
                contentDescription = "더보기",
                modifier =
                    Modifier
                        .clickable(onClick = onMoreClick),
            )
            EditDropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissDropdown,
                onDeleteClick = onDeleteClick,
                showEditItem = showEditItem,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteEditorReceiverListPreview() {
    AfternoteTheme {
        CompositionLocalProvider(
            DataProviderLocals.LocalAfternoteEditorDataProvider provides FakeAfternoteEditorDataProvider(),
        ) {
            val provider = DataProviderLocals.LocalAfternoteEditorDataProvider.current
            AfternoteEditorReceiverList(
                afternoteEditReceivers = provider.getAfternoteEditorReceivers(),
                events = AfternoteEditorReceiverCallbacks(),
            )
        }
    }
}
