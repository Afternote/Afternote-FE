package com.afternote.feature.receiver.presentation.recordsbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 받은 기록함 — 수신자가 등록한 발신자 카드 리스트.
 *
 * Welcome 의 "전달 받은 기록 확인하기" 에서 진입. 본인 확인 상태와 무관하게 진입 가능하며,
 * 발신자별 열람 신청 흐름은 발신자 상세 화면의 "열람 신청하기" 에서 시작한다 (이슈 #215).
 *
 * 빈 상태(14) / 채워진 상태(13) 를 sender 리스트 유무로 분기. 카드는 시안에 배치된 변형(Variant4)
 * 따름: 프로필 동그라미 + 이름 + 우측 chevron.
 */
@Composable
internal fun ReceivedRecordsScreen(
    onBackClick: () -> Unit,
    onAddSenderClick: () -> Unit,
    onSenderClick: (SenderEntry) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceivedRecordsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReceivedRecordsScreenContent(
        senders = state.senders,
        onBackClick = onBackClick,
        onAddSenderClick = onAddSenderClick,
        onSenderClick = onSenderClick,
        modifier = modifier,
    )
}
