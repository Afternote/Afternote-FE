package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.signup.scaffold.ProgressBarScaffold
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

@Composable
fun SignUpResidentNumberScreen(
    frontNumberState: TextFieldState,
    backNumberState: TextFieldState,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frontFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }

    // 앞자리 6자리 입력 완료 시 뒷자리로 포커스 자동 이동
    LaunchedEffect(frontNumberState) {
        snapshotFlow { frontNumberState.text }
            .filter { it.length == SignUpViewModel.RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT }
            .collectLatest { backFocusRequester.requestFocus() }
    }

    // 화면 진입 시 앞자리 필드에 포커스 → 키보드 표시
    LaunchedEffect(Unit) {
        frontFocusRequester.requestFocus()
    }

    ProgressBarScaffold(
        currentStep = 2,
        onBackClick = onBackClick,
        onNextClick = onNextClick,
        modifier = modifier,
        content = {
            Column(
                modifier =
                    Modifier
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 43.dp),
            ) {
                SignUpInputLabel(
                    text = stringResource(R.string.signup_resident_number_label),
                )

                Spacer(modifier = Modifier.height(18.dp))

                AfternoteTextField(
                    state = frontNumberState,
                    focusRequester = frontFocusRequester,
                    type =
                        TextFieldType.Variant8(
                            backState = backNumberState,
                            placeholder = stringResource(R.string.signup_resident_number_back_placeholder),
                            backFocusRequester = backFocusRequester,
                            frontFocusRequester = frontFocusRequester,
                        ),
                    placeholder = stringResource(R.string.signup_resident_number_placeholder),
                    keyboardType = KeyboardType.Number,
                    onImeAction = onNextClick,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SignUpResidentNumberScreenPreview() {
    AfternoteTheme {
        SignUpResidentNumberScreen(
            frontNumberState = rememberTextFieldState(),
            backNumberState = rememberTextFieldState(),
            onNextClick = {},
            onBackClick = {},
        )
    }
}
