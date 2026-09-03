package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.feature.onboarding.presentation.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

@Composable
fun SignUpResidentNumberScreen(
    viewModel: SignUpViewModel,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SignUpResidentNumberContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = rememberSignUpSnackbarHost(state, viewModel::onIntent),
        onNextClick = onNextClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

/** Step 2 — stateless 층. 프리뷰·screenshotTest·Robolectric 의 진입점이다. */
@Composable
internal fun SignUpResidentNumberContent(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frontNumberState = rememberTextFieldState(state.residentFrontNumber)
    val backNumberState = rememberTextFieldState(state.residentBackNumber)
    val frontFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }

    LaunchedEffect(frontNumberState) {
        snapshotFlow { frontNumberState.text.toString() }.collect { onIntent(SignUpIntent.UpdateResidentFrontNumber(it)) }
    }
    LaunchedEffect(backNumberState) {
        snapshotFlow { backNumberState.text.toString() }.collect { onIntent(SignUpIntent.UpdateResidentBackNumber(it)) }
    }

    // 앞자리 6자리 입력 완료 시 뒷자리로 포커스 자동 이동
    LaunchedEffect(frontNumberState) {
        snapshotFlow { frontNumberState.text }
            .filter { it.length == SignUpUiState.RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT }
            .collectLatest { backFocusRequester.requestFocus() }
    }

    // 화면 진입 시 앞자리 필드에 포커스 → 키보드 표시
    LaunchedEffect(Unit) {
        frontFocusRequester.requestFocus()
    }

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.onboarding_signup_title),
        actionButtonText = stringResource(R.string.onboarding_signup_next),
        onBackClick = onBackClick,
        onActionClick = onNextClick,
        modifier = modifier,
        isActionEnabled = state.isStep2NextEnabled,
        currentStep = SignUpStep.RESIDENT_NUMBER,
        totalSteps = SIGN_UP_TOTAL_STEPS,
        progressContentDescription = stringResource(R.string.onboarding_step_description, SignUpStep.RESIDENT_NUMBER),
        snackbarHostState = snackbarHostState,
        content = {
            Column(
                modifier =
                    Modifier
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 43.dp),
            ) {
                SignUpInputLabel(
                    text = stringResource(R.string.onboarding_signup_resident_number_label),
                )

                Spacer(modifier = Modifier.height(18.dp))

                AfternoteTextField(
                    state = frontNumberState,
                    focusRequester = frontFocusRequester,
                    type =
                        TextFieldType.Variant8(
                            backState = backNumberState,
                            placeholder = stringResource(R.string.onboarding_signup_resident_number_back_placeholder),
                            backFocusRequester = backFocusRequester,
                            frontFocusRequester = frontFocusRequester,
                        ),
                    placeholder = stringResource(R.string.onboarding_signup_resident_number_placeholder),
                    keyboardType = KeyboardType.Number,
                    onImeAction = { if (state.isStep2NextEnabled) onNextClick() },
                )
            }
        },
    )
}
