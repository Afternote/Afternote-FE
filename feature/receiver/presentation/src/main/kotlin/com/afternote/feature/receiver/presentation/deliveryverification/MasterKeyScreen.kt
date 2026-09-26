package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.mvi.ObserveSignal
import kotlinx.coroutines.launch

/**
 * 마스터 키 입력 화면(design 5) — 진행 인디케이터 2/3 + 단일 입력 + "다음" CTA (이슈 #215).
 *
 * `verify(masterKey)` 성공 시 SenderRegistry 에 masterKey·신원 결합 + 글로벌 헤더 저장 → [onVerified] 로 서류 업로드 단계 진입.
 *
 * 메모리 정책상 ViewModel 은 TextFieldState 미보유. UI 가 `rememberTextFieldState` 로 인스턴스를 들고 있다가 submit 시점에만 값을 전달.
 */
@Composable
internal fun MasterKeyScreen(
    senderId: String,
    onBackClick: () -> Unit,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MasterKeyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val masterKeyState = rememberTextFieldState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveSignal(
        signal = Unit.takeIf { uiState.isVerified },
        consumed = MasterKeyIntent.ConsumeVerified,
        onIntent = viewModel::onIntent,
        onSignal = { onVerified() },
    )

    val errorMessage =
        uiState.errorMessage?.asString()
    val snackbarScope = rememberCoroutineScope()
    ObserveSignal(
        signal = errorMessage,
        consumed = MasterKeyIntent.ConsumeError,
        onIntent = viewModel::onIntent,
    ) { message ->
        snackbarScope.launch { snackbarHostState.showSnackbar(message) }
    }

    MasterKeyScreenContent(
        masterKeyState = masterKeyState,
        isSubmitting = uiState.isSubmitting,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onSubmitClick = { viewModel.onIntent(MasterKeyIntent.Submit(senderId, masterKeyState.text.toString())) },
        modifier = modifier,
    )
}
