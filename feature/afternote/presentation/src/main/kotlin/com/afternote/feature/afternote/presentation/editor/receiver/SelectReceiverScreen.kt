package com.afternote.feature.afternote.presentation.editor.receiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.receiver.ReceiverSelectItem
import com.afternote.core.ui.receiver.ReceiverSelectScreen
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/**
 * 애프터노트 에디터의 수신자 선택 화면 (#540, 시안 3631:24820).
 *
 * 검색·초성 인덱스·복수 선택·완료 UI 는 공용 [ReceiverSelectScreen](#791) 이 그리고,
 * 여기서는 에디터 모델 매핑과 로딩·조회 실패·빈 목록 상태만 소유한다.
 *
 * 한 번 진입해 여러 명을 확정할 수 있다 — 완료는 선택한 id 전체를 한 번에 돌려준다 (#1426).
 *
 * 상태 body 를 core:ui 로 올리는 조건(#1427 본문)은 «두 번째 작성 플로우가 이 컴포넌트에
 * 붙는 시점» 이다. 착수 시점(2026-08-30)의 `listReplacement` 소비자는 여전히 애프터노트
 * 하나뿐이라(설정 `ReceiverListScreen` 은 슬롯을 쓰지 않는다) 여기에 둔다.
 */
@Composable
internal fun SelectReceiverScreen(
    uiState: SelectReceiverUiState,
    onBackClick: () -> Unit,
    onReceiverToggle: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onConfirmClick: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    ReceiverSelectScreen(
        receivers =
            remember(uiState.receivers) {
                uiState.receivers.map { ReceiverSelectItem(id = it.id, name = it.name, relation = it.label) }
            },
        selectedReceiverIds = uiState.selectedReceiverIds,
        onReceiverToggle = onReceiverToggle,
        onBackClick = onBackClick,
        onConfirmClick = onConfirmClick,
        modifier = modifier,
        listReplacement =
            when {
                uiState.loadFailed -> {
                    { SelectReceiverLoadFailed(onRetryClick = onRetryClick) }
                }

                uiState.isLoading && uiState.receivers.isEmpty() -> {
                    { LoadingBody() }
                }

                uiState.receivers.isEmpty() -> {
                    { SelectReceiverEmpty() }
                }

                else -> {
                    null
                }
            },
    )
}

/**
 * 수신자 0건 빈 상태 (#1427, 확정 시안 4163:20979).
 *
 * 시안 치수: 제목/설명 8dp 간격 → 56dp → 일러스트(134dp).
 * 일러스트는 시안과 같은 그림인 core:ui [ProfileImage] 의 placeholder 를 기본 크기(134dp)로 쓴다.
 */
@Composable
private fun SelectReceiverEmpty() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp),
    ) {
        Text(
            text = stringResource(R.string.afternote_select_receiver_empty),
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.black,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.afternote_select_receiver_empty_description),
            // 시안 4163:20979 의 H3 는 weight 400 인데 레포 `h3` 토큰은 Bold 다. 시안대로 되돌린다 —
            // 토큰 자체를 바꾸면 기존 h3 사용처가 전부 얇아진다.
            style = AfternoteDesign.typography.h3.copy(fontWeight = FontWeight.Normal),
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(modifier = Modifier.height(56.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ProfileImage()
        }
    }
}

@Composable
private fun SelectReceiverLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.afternote_select_receiver_load_failed),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray8,
            textAlign = TextAlign.Center,
        )
        AfternoteButton(
            text = stringResource(R.string.afternote_select_receiver_retry),
            onClick = onRetryClick,
            modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
        )
    }
}
