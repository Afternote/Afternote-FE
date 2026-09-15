package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopupHost
import kotlinx.coroutines.launch

/**
 * 본인 확인 이메일 인증(designs 3·4) — 이메일 + 인증번호 입력 화면 (이슈 #215).
 *
 * 디자인 3 (입력 전) 과 4 (인증번호 발송 후 안내 메시지 표시) 는 동일 화면. 발송 직후 두 입력 필드 아래
 * 강조 안내 텍스트가 나타난다.
 *
 * 인증번호 발송·검증은 실 API(`receiver-auth/email` 계열) — 서버 거절 안내 문구(이메일 미등록 등) 는
 * 스낵바로 노출된다 (#407). 서버·네트워크 실패는 스낵바가 아니라 공통 오류 팝업이 맡는다 (#446) —
 * 사용자가 할 일이 재시도뿐이라 스스로 사라지는 안내로는 그 액션을 줄 자리가 없다.
 *
 * `senderId` 는 [MasterKeyScreen] 과 같은 규약으로 parent backStackEntry 의
 * [DeliveryVerificationFlowViewModel] 에서 받아 검증 성공 시점의 발신자별 캐시 기록에 쓴다 (#597).
 */
@Composable
internal fun IdentityVerificationEmailScreen(
    senderId: String,
    onBackClick: () -> Unit,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IdentityVerificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emailState = rememberTextFieldState()
    val codeState = rememberTextFieldState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(emailState) {
        snapshotFlow { emailState.text.toString() }.collect({ value -> viewModel.onIntent(IdentityVerificationIntent.UpdateEmail(value)) })
    }
    LaunchedEffect(codeState) {
        snapshotFlow { codeState.text.toString() }.collect({ value -> viewModel.onIntent(IdentityVerificationIntent.UpdateCode(value)) })
    }

    ObserveSignal(
        signal = Unit.takeIf { uiState.isVerified },
        consumed = IdentityVerificationIntent.ConsumeVerified,
        onIntent = viewModel::onIntent,
        onSignal = { onVerified() },
    )

    val errorMessage =
        uiState.errorMessage?.asString()
    val snackbarScope = rememberCoroutineScope()
    ObserveSignal(
        signal = errorMessage,
        consumed = IdentityVerificationIntent.ConsumeError,
        onIntent = viewModel::onIntent,
    ) { message ->
        snackbarScope.launch { snackbarHostState.showSnackbar(message) }
    }

    IdentityVerificationEmailScreenContent(
        uiState = uiState,
        emailState = emailState,
        codeState = codeState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onRequestCode = { viewModel.onIntent(IdentityVerificationIntent.RequestCode) },
        onVerifyAndProceed = { viewModel.onIntent(IdentityVerificationIntent.Verify(senderId)) },
        modifier = modifier,
    )

    ReceiverErrorPopupHost(
        popup = uiState.errorPopup,
        onRetry = { viewModel.onIntent(IdentityVerificationIntent.RetryFailedRequest) },
        onDismiss = { viewModel.onIntent(IdentityVerificationIntent.DismissErrorPopup) },
    )
}
