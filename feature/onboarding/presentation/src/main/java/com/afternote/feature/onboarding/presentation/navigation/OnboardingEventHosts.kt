package com.afternote.feature.onboarding.presentation.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.findaccount.FindIdUiState
import com.afternote.feature.onboarding.presentation.findaccount.FindIdViewModel
import com.afternote.feature.onboarding.presentation.signup.SignUpUiState
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel
import com.afternote.feature.onboarding.presentation.snackbarMessage
import com.afternote.feature.onboarding.presentation.toDisplay

/**
 * 아이디 찾기 화면의 Snackbar 호스트 + 단발성 에러 신호 처리.
 *
 * [FindIdUiState.failure]의 공통 표시 결정 중 스낵바 몫만 소비한다.
 */
@Composable
internal fun rememberFindIdEventHost(
    viewModel: FindIdViewModel,
    uiState: FindIdUiState,
): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }
    // VM 이 UiText 로 폴백까지 확정해 두므로 빈 문구가 도달하지 않는다.
    val snackbarMessage =
        uiState.failure
            .toDisplay()
            .snackbarMessage
            ?.asString()

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short,
            )
            viewModel.onErrorConsumed()
        }
    }
    return snackbarHostState
}

/**
 * SignUp Step 화면 공통의 Snackbar 호스트 + UI state 단발성 신호 처리.
 *
 * 각 Step entry 에서 호출해 [SignUpUiState.failure] / [SignUpUiState.isNameRequired]
 * 를 일관되게 snackbar 로 노출하고, Step 1 의 경우 [onNavigateToResidentNumber] 콜백으로
 * [SignUpUiState.shouldNavigateToResidentNumber] = true 시점에 네비게이트한다.
 *
 * sealed Event Channel 대신 UiState 의 nullable/boolean 신호 + [LaunchedEffect] + on*Consumed 패턴으로 통일
 * (Google 공식 가이드 — ViewModel events should always result in a UI state update).
 */
@Composable
internal fun rememberSignUpEventHost(
    viewModel: SignUpViewModel,
    onNavigateToResidentNumber: (() -> Unit)? = null,
): SnackbarHostState {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val nameRequiredMessage = stringResource(R.string.onboarding_signup_name_required)

    LaunchedEffect(uiState.shouldNavigateToResidentNumber) {
        if (uiState.shouldNavigateToResidentNumber && onNavigateToResidentNumber != null) {
            onNavigateToResidentNumber()
            viewModel.onResidentNumberNavigatedConsumed()
        }
    }

    LaunchedEffect(uiState.isNameRequired) {
        if (uiState.isNameRequired) {
            snackbarHostState.showSnackbar(
                message = nameRequiredMessage,
                duration = SnackbarDuration.Short,
            )
            viewModel.onNameRequiredConsumed()
        }
    }

    val snackbarMessage =
        uiState.failure
            .toDisplay()
            .snackbarMessage
            ?.asString()
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short,
            )
            viewModel.onErrorConsumed()
        }
    }

    return snackbarHostState
}
