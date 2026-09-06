package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.receiver.ReceiverSelectItem
import com.afternote.core.ui.receiver.ReceiverSelectScreen

/**
 * 설정의 수신자 목록(선택) 화면 — 공용 [ReceiverSelectScreen] 소비 (#791).
 *
 * 검색·초성 인덱스·단일 선택·완료 UI 는 공용 화면이 그리고, 여기서는
 * 설정 모델 매핑과 화면 내 선택 상태만 소유한다. 공개 시그니처는 추출 전과 같다.
 */
@Composable
fun ReceiverListScreen(
    receivers: List<ReceiverListItem>,
    onBackClick: () -> Unit,
    onConfirmClick: (ReceiverListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }

    ReceiverSelectScreen(
        // 유일하게 공용 기본값("수신자 선택")을 벗어나는 소비자다 — #631 이 이 화면을
        // 관리 화면으로 바꾸면 선택 컴포넌트 소비 자체가 사라진다.
        title = "수신자 목록",
        receivers =
            remember(receivers) {
                receivers.map { ReceiverSelectItem(id = it.receiverId, name = it.name, relation = it.relation) }
            },
        selectedReceiverId = selectedId,
        onReceiverToggle = { receiverId ->
            selectedId = if (selectedId == receiverId) null else receiverId
        },
        onBackClick = onBackClick,
        onConfirmClick = { receiverId ->
            receivers.find { it.receiverId == receiverId }?.let(onConfirmClick)
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun ReceiverListScreenPrev() {
    ReceiverListScreen(
        receivers =
            listOf(
                ReceiverListItem(receiverId = 1L, name = "박경민", relation = "친구"),
                ReceiverListItem(receiverId = 2L, name = "김철수", relation = "가족"),
                ReceiverListItem(receiverId = 3L, name = "이영희", relation = "연인"),
            ),
        onBackClick = {},
        onConfirmClick = { _ -> },
    )
}
