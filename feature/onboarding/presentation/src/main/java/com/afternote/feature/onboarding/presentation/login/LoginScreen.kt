package com.afternote.feature.onboarding.presentation.login.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.form.AfternoteTextField
import com.afternote.core.ui.form.PasswordMaskTransformation
import com.afternote.core.ui.scaffold.topbar.HomeTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.login.BottomButtons
import com.afternote.feature.onboarding.presentation.login.LoginButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    emailState: TextFieldState,
    passwordState: TextFieldState,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            HomeTopBar()
        },
        containerColor = AfternoteDesign.colors.white,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 아이디 (이메일) 입력 필드
            AfternoteTextField(
                state = emailState,
                placeholder = stringResource(R.string.login_email_label),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                modifier =
                    Modifier.semantics { contentType = ContentType.Username },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 비밀번호 입력 필드
            AfternoteTextField(
                state = passwordState,
                placeholder = stringResource(R.string.login_password_label),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                outputTransformation = PasswordMaskTransformation,
                modifier =
                    Modifier.semantics { contentType = ContentType.Password },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 로그인 버튼
            LoginButton(
                text = stringResource(R.string.login_button),
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = emailState.text.isNotBlank() && passwordState.text.isNotEmpty(),
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 하단 버튼 영역
            BottomButtons(
                onSignUpClick = onSignUpClick,
                onKakaoLoginClick = onKakaoLoginClick,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    AfternoteTheme {
        LoginScreen(
            emailState = rememberTextFieldState(),
            passwordState = rememberTextFieldState(),
            onLoginClick = {},
            onSignUpClick = {},
            onKakaoLoginClick = {},
        )
    }
}
