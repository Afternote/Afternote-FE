package com.afternote.feature.receiver.presentation.senderdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.receiver.presentation.R

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
 * [SenderDetailUiState.Success.shouldOpenReceiverHome] 를 true 로 갱신. 본 화면이 [LaunchedEffect] 로 받아
 * [onOpenReceiverHome] (순수 네비게이션) 호출 후 [SenderDetailViewModel.onOpenReceiverHomeConsumed] 로 reset.
 */
@Composable
fun SenderDetailScreen(
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
        viewModel.refreshOnReturn()
    }

    LaunchedEffect(shouldOpenReceiverHome) {
        if (shouldOpenReceiverHome) {
            onOpenReceiverHome()
            viewModel.onOpenReceiverHomeConsumed()
        }
    }

    SenderDetailScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRequestVerification = onRequestVerification,
        onOpenReceiverHome = viewModel::openReceiverHome,
        modifier = modifier,
    )
}

@Composable
internal fun SenderDetailScreenContent(
    uiState: SenderDetailUiState,
    onBackClick: () -> Unit,
    onRequestVerification: () -> Unit,
    onOpenReceiverHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.receiver_sender_detail_top_bar_title),
                onBackClick = onBackClick,
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
        ) {
            when (uiState) {
                SenderDetailUiState.Loading -> {
                    // 디자인에 loading 시각 표시 없음 — 빈 화면 유지.
                }

                SenderDetailUiState.SenderNotFound -> {
                    CenteredMessage(
                        text = stringResource(R.string.receiver_sender_detail_not_found),
                    )
                }

                is SenderDetailUiState.StatusLoadFailed -> {
                    SuccessBody(
                        displayName = uiState.displayName,
                        verification = null,
                        requestedAt = null,
                        approvedAt = null,
                        errorMessage = stringResource(R.string.receiver_sender_detail_status_load_failed),
                        onRequestVerification = onRequestVerification,
                        onOpenReceiverHome = onOpenReceiverHome,
                    )
                }

                is SenderDetailUiState.Success -> {
                    SuccessBody(
                        displayName = uiState.displayName,
                        verification = uiState.verification,
                        requestedAt = uiState.requestedAt,
                        approvedAt = uiState.approvedAt,
                        errorMessage = null,
                        onRequestVerification = onRequestVerification,
                        onOpenReceiverHome = onOpenReceiverHome,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessBody(
    displayName: String,
    verification: SenderVerificationState?,
    requestedAt: String?,
    approvedAt: String?,
    errorMessage: String?,
    onRequestVerification: () -> Unit,
    onOpenReceiverHome: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(39.dp))

        Image(
            painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_ic_profile_placeholder),
            contentDescription = null,
            modifier =
                Modifier
                    .size(134.dp)
                    .clip(CircleShape),
        )

        Spacer(modifier = Modifier.height(12.5.dp))

        Text(
            text = displayName,
            // 시안 실측(정본 페이지 「정리 Screen Design」, Figma REST): fontSize 32 / lineHeight 32px = 100%.
            // 신청 전 https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-74206
            // 승인 후 https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-74250
            // 두 노드가 든 화면 프레임(4327:74200 · 4327:74244)은 레이어명이 「현재 수신인 목록」이지만
            // 복붙 잔재고, 내용은 이 화면이 맞다 — 원형 프로필 134 아래 이름, 그 아래 정보 박스 4행.
            //
            // bodyLargeB 는 18/24(133%) 라 fontSize 만 덮으면 행간 24sp 가 그대로 상속돼 글자(32sp)
            // 보다 작아진다 — 이름이 두 줄로 접히면 줄이 겹친다. 그래서 lineHeight 를 함께 적는다.
            // 대조 완료 — 다시 재지 않아도 된다 (#1444 · #1486).
            style =
                AfternoteDesign.typography.bodyLargeB.copy(fontSize = 32.sp, lineHeight = 32.sp),
            color = AfternoteDesign.colors.gray9,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(56.dp))

        SenderInfoBox(
            verification = verification,
            requestedAt = requestedAt,
            approvedAt = approvedAt,
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // verification 이 null(상태 조회 실패) 인 경우는 액션 분기 근거가 없으므로 버튼 비노출.
        verification?.let {
            VerificationActionButton(
                state = it,
                onRequestVerification = onRequestVerification,
                onOpenReceiverHome = onOpenReceiverHome,
            )
            Spacer(modifier = Modifier.height(125.5.dp))
        }
    }
}

@Composable
private fun SenderInfoBox(
    verification: SenderVerificationState?,
    requestedAt: String?,
    approvedAt: String?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InfoRow(
            label = stringResource(R.string.receiver_sender_detail_row_records),
            value = stringResource(R.string.receiver_sender_detail_records_kept),
            isPlaceholder = false,
        )
        InfoRow(
            label = stringResource(R.string.receiver_sender_detail_row_status),
            value = verification.statusValue() ?: "-",
            isPlaceholder = verification == null,
        )
        InfoRow(
            label = stringResource(R.string.receiver_sender_detail_row_requested_at),
            value =
                requestedAt
                    ?: stringResource(R.string.receiver_sender_detail_no_request_record),
            isPlaceholder = requestedAt == null,
        )
        InfoRow(
            label = stringResource(R.string.receiver_sender_detail_row_approved_at),
            value =
                approvedAt
                    ?: stringResource(R.string.receiver_sender_detail_no_approval_record),
            isPlaceholder = approvedAt == null,
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isPlaceholder: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AfternoteDesign.colors.white)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = AfternoteDesign.typography.bodyBase,
            color =
                if (isPlaceholder) AfternoteDesign.colors.gray5 else AfternoteDesign.colors.gray8,
        )
    }
}

@Composable
private fun VerificationActionButton(
    state: SenderVerificationState,
    onRequestVerification: () -> Unit,
    onOpenReceiverHome: () -> Unit,
) {
    when (state) {
        SenderVerificationState.NotRequested,
        SenderVerificationState.Rejected,
        -> {
            AfternoteButton(
                text = stringResource(R.string.receiver_sender_detail_request_verification),
                onClick = onRequestVerification,
                type = AfternoteButtonType.Default,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SenderVerificationState.Pending -> {
            AfternoteButton(
                text = stringResource(R.string.receiver_sender_detail_pending_button),
                onClick = {},
                type = AfternoteButtonType.Un,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SenderVerificationState.Approved -> {
            AfternoteButton(
                text = stringResource(R.string.receiver_sender_detail_open_records),
                onClick = onOpenReceiverHome,
                type = AfternoteButtonType.Default,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray6,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SenderVerificationState?.statusValue(): String? {
    val state = this ?: return null
    val res =
        when (state) {
            SenderVerificationState.NotRequested -> R.string.receiver_sender_detail_status_unavailable
            SenderVerificationState.Pending -> R.string.receiver_sender_detail_status_pending
            SenderVerificationState.Approved -> R.string.receiver_sender_detail_status_available
            SenderVerificationState.Rejected -> R.string.receiver_sender_detail_status_rejected
        }
    return stringResource(res)
}
