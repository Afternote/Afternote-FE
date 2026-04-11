package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.Label
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.addFocusCleaner
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

private const val FRONT_NUMBER_LENGTH = 6
private const val BACK_NUMBER_LENGTH = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpResidentNumberScreen(
    currentStep: Int,
    frontNumberState: TextFieldState,
    backNumberState: TextFieldState,
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
        containerColor = AfternoteDesign.colors.white,
    ) { innerPadding ->
        ResidentNumberContent(
            currentStep = currentStep,
            frontNumberState = frontNumberState,
            backNumberState = backNumberState,
            onNextClick = onNextClick,
            progressDescription = progressDescription,
            modifier =
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
        )
    }
}

@Composable
private fun ResidentNumberContent(
    currentStep: Int,
    frontNumberState: TextFieldState,
    backNumberState: TextFieldState,
    onNextClick: () -> Unit,
    progressDescription: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // 진행바
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / SIGN_UP_TOTAL_STEPS },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .semantics { contentDescription = progressDescription },
            color = AfternoteDesign.colors.gray9,
            trackColor = AfternoteDesign.colors.gray3,
            strokeCap = StrokeCap.Square,
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

            Spacer(modifier = Modifier.height(8.dp))

            AfternoteTextField(
                state = rememberTextFieldState(),
                type = TextFieldType.Variant8,
                placeholder = "주민등록번호",
            )
        }

        // 다음 버튼
        val isNextEnabled =
            frontNumberState.text.length == FRONT_NUMBER_LENGTH &&
                backNumberState.text.length == BACK_NUMBER_LENGTH
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
                if (isNextEnabled) {
                    AfternoteButtonType.Default
                } else {
                    AfternoteButtonType.Un
                },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpResidentNumberScreenPreview() {
    AfternoteTheme {
        SignUpResidentNumberScreen(
            currentStep = 2,
            frontNumberState = rememberTextFieldState(),
            backNumberState = rememberTextFieldState(),
            onNextClick = {},
            onBackClick = {},
        )
    }
}
