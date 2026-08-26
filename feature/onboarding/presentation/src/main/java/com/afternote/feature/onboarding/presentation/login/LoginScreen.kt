package com.afternote.feature.onboarding.presentation.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.popup.NetworkErrorPopup
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.onboarding.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    initialEmail: String,
    initialPassword: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onFindAccountClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onRetryLogin: () -> Unit,
    onNetworkErrorDismiss: () -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    hasCredentialError: Boolean = false,
    showNetworkErrorPopup: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val emailState = rememberTextFieldState(initialEmail)
    val passwordState = rememberTextFieldState(initialPassword)

    LaunchedEffect(emailState) {
        snapshotFlow { emailState.text.toString() }.collect(onEmailChange)
    }
    LaunchedEffect(passwordState) {
        snapshotFlow { passwordState.text.toString() }.collect(onPasswordChange)
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.onboarding_login_top_bar_title),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
        bottomBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 45.dp),
            ) {
                SocialLoginGroup(
                    onSignUpClick = onSignUpClick,
                    onFindAccountClick = onFindAccountClick,
                    onKakaoLoginClick = onKakaoLoginClick,
                    onGoogleLoginClick = onGoogleLoginClick,
                )
            }
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .addFocusCleaner(focusManager)
                    .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(39.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 아이디 (이메일) 입력 필드
                AfternoteTextField(
                    state = emailState,
                    modifier =
                        Modifier.semantics { contentType = ContentType.Username },
                    placeholder = stringResource(R.string.onboarding_login_email_label),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )

                // 비밀번호 필드와 인라인 안내(시안 3628:23437)를 6dp 로 묶는다 — 바깥 8dp 와 달라
                // 별도 Column. 문구 스타일은 아이디 찾기 인라인(FindIdScreen)과 동일 토큰.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AfternoteTextField(
                        state = passwordState,
                        modifier =
                            Modifier.semantics { contentType = ContentType.Password },
                        placeholder = stringResource(R.string.onboarding_login_password_label),
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            if (!isLoading) {
                                focusManager.clearFocus()
                                onLoginClick()
                            }
                        },
                        isError = hasCredentialError,
                    )

                    if (hasCredentialError) {
                        Text(
                            text = stringResource(R.string.onboarding_login_invalid_credentials),
                            style = AfternoteDesign.typography.captionLargeB,
                            color = AfternoteDesign.colors.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 로그인 버튼
            AfternoteButton(
                text = stringResource(R.string.onboarding_login_button),
                onClick = {
                    if (!isLoading) {
                        focusManager.clearFocus()
                        onLoginClick()
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                type = AfternoteButtonType.Default,
                isLoading = isLoading,
            )
        }
    }

    if (showNetworkErrorPopup) {
        NetworkErrorPopup(
            onRetry = onRetryLogin,
            onDismiss = onNetworkErrorDismiss,
        )
    }
}

/**
 * 로그인 화면 하단 소셜 로그인 그룹.
 *
 * `LoginScreen`에서만 쓰이므로 외부 파일로 분리하지 않고 `private`로 캡슐화한다.
 */
@Composable
private fun SocialLoginGroup(
    onSignUpClick: () -> Unit,
    onFindAccountClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 간편 회원가입하기 버튼
        AfternoteButton(
            text = stringResource(R.string.onboarding_login_signup_simple),
            type = AfternoteButtonType.Plain,
            onClick = {
                focusManager.clearFocus()
                onSignUpClick()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 카카오 로그인 버튼
        Button(
            onClick = {
                focusManager.clearFocus()
                onKakaoLoginClick()
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE500),
                ),
            contentPadding = PaddingValues(0.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding_kakao_login_large_wide_1),
                contentDescription = stringResource(R.string.onboarding_login_kakao),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillBounds,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 구글 로그인 버튼
        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                onGoogleLoginClick()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            // 💡 외곽선과 내부 컨텐츠(Text) 사이의 패딩을 정확히 12.dp로 고정합니다.
            contentPadding = PaddingValues(12.dp),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    // 근사 토큰: 원본 #F2F2F2 → 최근접 gray2(#EEEEEE, 채널당 -4)
                    containerColor = AfternoteDesign.colors.gray2,
                    contentColor = AfternoteDesign.colors.gray8,
                ),
            border = BorderStroke(1.dp, AfternoteDesign.colors.gray3),
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_img_google_logo),
                    contentDescription = stringResource(R.string.onboarding_content_description_google_login),
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = stringResource(R.string.onboarding_login_google),
                style = AfternoteDesign.typography.captionLargeB,
                color = AfternoteDesign.colors.gray8,
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 아이디/비밀번호 찾기
        Text(
            text = stringResource(R.string.onboarding_login_find_account),
            style =
                AfternoteDesign.typography.captionLargeR.copy(
                    color = AfternoteDesign.colors.gray6,
                    textDecoration = TextDecoration.Underline,
                ),
            modifier =
                Modifier.clickable {
                    focusManager.clearFocus()
                    onFindAccountClick()
                },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    AfternoteTheme {
        LoginScreen(
            initialEmail = "",
            initialPassword = "",
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onSignUpClick = {},
            onFindAccountClick = {},
            onKakaoLoginClick = {},
            onGoogleLoginClick = {},
            onRetryLogin = {},
            onNetworkErrorDismiss = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
