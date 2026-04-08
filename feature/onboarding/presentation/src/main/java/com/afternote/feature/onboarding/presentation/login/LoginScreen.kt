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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.topbar.HomeTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.login.BottomButtons
import com.afternote.feature.onboarding.presentation.login.LoginButton
import com.afternote.feature.onboarding.presentation.login.MyInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
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
            MyInputField(
                label = stringResource(R.string.login_email_label),
                value = email,
                onValueChange = onEmailChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Username },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 비밀번호 입력 필드
            MyInputField(
                label = stringResource(R.string.login_password_label),
                value = password,
                onValueChange = onPasswordChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Password },
                isPassword = true,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 로그인 버튼
            LoginButton(
                text = stringResource(R.string.login_button),
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotEmpty(),
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
            email = "",
            onEmailChange = {},
            password = "",
            onPasswordChange = {},
            onLoginClick = {},
            onSignUpClick = {},
            onKakaoLoginClick = {},
        )
    }
}
