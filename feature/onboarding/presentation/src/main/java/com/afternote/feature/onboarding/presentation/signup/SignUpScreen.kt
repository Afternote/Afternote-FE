package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.mvi.ObserveFlag

@Composable
internal fun SignUpScreen(
    viewModel: SignUpViewModel,
    onNavigateToResidentNumber: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberSignUpSnackbarHost(state, viewModel::onIntent)

    ObserveFlag(
        raised = state.shouldNavigateToResidentNumber,
        consumed = SignUpIntent.ConsumeResidentNumberNavigation,
        onIntent = viewModel::onIntent,
        onRaised = onNavigateToResidentNumber,
    )

    SignUpContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
