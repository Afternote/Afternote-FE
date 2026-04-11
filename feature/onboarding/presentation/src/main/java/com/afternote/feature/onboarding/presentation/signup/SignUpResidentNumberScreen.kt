package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.Label
import com.afternote.core.ui.StepProgressBar
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.addFocusCleaner
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

private const val FRONT_NUMBER_LENGTH = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpResidentNumberScreen(
    currentStep: Int,
    frontNumberState: TextFieldState,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val progressDescription =
        stringResource(R.string.signup_progress_description, currentStep, SIGN_UP_TOTAL_STEPS)

    val backFocusRequester = remember { FocusRequester() }

    // 앞자리 6자리 입력 완료 시 뒷자리로 포커스 자동 이동
    LaunchedEffect(frontNumberState) {
        snapshotFlow { frontNumberState.text }
            .filter { it.length == FRONT_NUMBER_LENGTH }
            .collectLatest { backFocusRequester.requestFocus() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.signup_title),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        ResidentNumberContent(
            currentStep = currentStep,
            onNextClick = onNextClick,
            progressDescription = progressDescription,
            Modifier
                .padding(innerPadding),
        )
    }
}

@Composable
private fun ResidentNumberContent(
    currentStep: Int,
    onNextClick: () -> Unit,
    progressDescription: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        StepProgressBar(
            currentStep = currentStep,
            totalSteps = SIGN_UP_TOTAL_STEPS,
            contentDescription = progressDescription,
        )

        // 입력 폼
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .addFocusCleaner(focusManager)
                    .padding(horizontal = 24.dp)
                    .padding(top = 45.dp),
        ) {
            Label(
                text = stringResource(R.string.signup_resident_number_label),
            )

            Spacer(modifier = Modifier.height(18.dp))

            AfternoteTextField(
                state = rememberTextFieldState(),
                type = TextFieldType.Variant8,
                placeholder = "주민등록번호",
            )
        }

        // 다음 버튼
        AfternoteButton(
            text = stringResource(R.string.signup_next),
            onClick = {
                focusManager.clearFocus()
                onNextClick()
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .height(48.dp),
            type =
                AfternoteButtonType.Default,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpResidentNumberScreenPreview() {
    AfternoteTheme {
        SignUpResidentNumberScreen(
            currentStep = 2,
            frontNumberState = rememberTextFieldState(),
            onNextClick = {},
            onBackClick = {},
        )
    }
}
