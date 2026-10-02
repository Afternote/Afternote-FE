package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SignUpResidentNumberScreen(
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
