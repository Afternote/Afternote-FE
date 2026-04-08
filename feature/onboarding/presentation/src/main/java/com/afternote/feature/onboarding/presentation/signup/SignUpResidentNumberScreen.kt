package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.AfternoteTextField
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
                onBackClick = onBackClick,
            )
        },
        containerColor = AfternoteDesign.colors.white,
    ) { innerPadding ->
        ResidentNumberContent(
            currentStep = currentStep,
            frontNumberState = frontNumberState,
            backNumberState = backNumberState,
            backFocusRequester = backFocusRequester,
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
    backFocusRequester: FocusRequester,
    onNextClick: () -> Unit,
    progressDescription: String,
    modifier: Modifier = Modifier,
) {
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
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.signup_resident_number_label),
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ResidentNumberInputField(
                frontState = frontNumberState,
                backState = backNumberState,
                backFocusRequester = backFocusRequester,
            )
        }

        // 다음 버튼
        val isNextEnabled =
            frontNumberState.text.length == FRONT_NUMBER_LENGTH &&
                backNumberState.text.length == BACK_NUMBER_LENGTH
        AfternoteButton(
            text = stringResource(R.string.signup_next),
            onClick = onNextClick,
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

@Composable
private fun ResidentNumberInputField(
    frontState: TextFieldState,
    backState: TextFieldState,
    backFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val numberOnlyTransformation =
        InputTransformation {
            if (!asCharSequence().all { it.isDigit() }) revertAllChanges()
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray3,
                    shape = RoundedCornerShape(8.dp),
                ).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 앞자리 6자리
        AfternoteTextField(
            state = frontState,
            modifier = Modifier.weight(1f),
            placeholder = stringResource(R.string.signup_resident_number_placeholder),
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next,
            inputTransformation = InputTransformation.maxLength(FRONT_NUMBER_LENGTH).then(numberOnlyTransformation),
            lineLimits = TextFieldLineLimits.SingleLine,
            showOutline = false,
            containerColor = Color.Transparent,
            contentPadding = PaddingValues(0.dp),
            minHeight = 56.dp,
            expandTextAreaToAvailableWidth = true,
            textStyle =
                AfternoteDesign.typography.textField.copy(
                    color = AfternoteDesign.colors.gray9,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp,
                ),
        )

        // 구분 기호
        Text(
            text = "—",
            style = AfternoteDesign.typography.textField,
            color = AfternoteDesign.colors.gray4,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        // 뒷자리 (1자리 입력 + 마스킹)
        AfternoteTextField(
            state = backState,
            modifier = Modifier.weight(1f),
            focusRequester = backFocusRequester,
            placeholder = stringResource(R.string.signup_resident_number_back_placeholder),
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            inputTransformation = InputTransformation.maxLength(BACK_NUMBER_LENGTH).then(numberOnlyTransformation),
            lineLimits = TextFieldLineLimits.SingleLine,
            showOutline = false,
            containerColor = Color.Transparent,
            contentPadding = PaddingValues(0.dp),
            minHeight = 56.dp,
            minWidth = 16.dp,
            suffix = {
                Text(
                    text = stringResource(R.string.signup_resident_number_mask),
                    style =
                        AfternoteDesign.typography.textField.copy(
                            fontSize = 12.sp,
                            letterSpacing = 4.sp,
                        ),
                    color = AfternoteDesign.colors.gray9,
                    modifier = Modifier.padding(start = 4.dp),
                )
            },
            textStyle =
                AfternoteDesign.typography.textField.copy(
                    color = AfternoteDesign.colors.gray9,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpResidentNumberScreenPreview() {
    AfternoteTheme {
        ResidentNumberContent(
            currentStep = 2,
            frontNumberState = rememberTextFieldState(),
            backNumberState = rememberTextFieldState(),
            backFocusRequester = remember { FocusRequester() },
            onNextClick = {},
            progressDescription = "회원가입 진행도 4단계 중 2단계",
        )
    }
}
