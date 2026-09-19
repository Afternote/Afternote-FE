package com.afternote.feature.receiver.presentation.senderdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.mvi.ObserveSignal

/**
 * 발신자 상세(designs 11·12) — 받은 기록함 카드 클릭 진입 (이슈 #215).
 *
 * 레이아웃 (시안 그대로):
 * 1. TopBar 제목 "받은 기록함" (발신자 이름이 아님).
 * 2. 가운데 큰 프로필 placeholder + 발신자 이름.
 * 3. 정보 박스 4 행: 기록 / 상태 / 신청일 / 승인일.
 * 4. 하단 CTA: "열람 신청하기" (NotRequested/Pending/Rejected) 또는 "기록 열람하기" (Approved).
 *
 * "기록 열람하기" 클릭 시 ViewModel 이 글로벌 헤더에 masterKey 를 복원한 뒤
 * [SenderDetailUiState.Success.shouldOpenReceiverHome] 를 true 로 갱신. 본 화면이 [ObserveSignal] 로 받아
 * [onOpenReceiverHome] (순수 네비게이션) 호출 후 [SenderDetailIntent.ConsumeOpenReceiverHome] 로 reset.
 */
@Composable
internal fun SenderDetailScreen(
    onBackClick: () -> Unit,
    onRequestVerification: () -> Unit,
    onOpenReceiverHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SenderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val shouldOpenReceiverHome = (uiState as? SenderDetailUiState.Success)?.shouldOpenReceiverHome == true

    // 열람 신청 흐름에서 복귀하면 상태를 다시 조회한다 — 신청 직후 돌아온 화면이 "신청 전" 을
    // 그대로 보여주지 않게 한다 (#701). 로딩을 방출하지 않는 refreshOnReturn() 을 쓴다.
    // 첫 진입의 ON_RESUME 스킵(진입은 init 로드가 담당)과 실행 중 로드와의 중복 차단은
    // VM 이 판단한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(SenderDetailIntent.RefreshOnReturn)
    }

    ObserveSignal(
        signal = Unit.takeIf { shouldOpenReceiverHome },
        consumed = SenderDetailIntent.ConsumeOpenReceiverHome,
        onIntent = viewModel::onIntent,
        onSignal = { onOpenReceiverHome() },
    )

    SenderDetailScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRequestVerification = onRequestVerification,
        onOpenReceiverHome = { viewModel.onIntent(SenderDetailIntent.OpenReceiverHome) },
        modifier = modifier,
    )
}
