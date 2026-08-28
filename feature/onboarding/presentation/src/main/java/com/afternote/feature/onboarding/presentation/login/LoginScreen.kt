package com.afternote.feature.onboarding.presentation.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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

        // 카카오 로그인 버튼 — 배경·심볼만 브랜드 고정색, 프레임 규격은 구글·이메일 버튼과 동일.
        SocialLoginButton(
            text = stringResource(R.string.onboarding_login_kakao),
            iconPainter = painterResource(R.drawable.onboarding_ic_kakao_symbol),
            containerColor = KakaoContainerColor,
            contentColor = KakaoContentColor,
            onClick = {
                focusManager.clearFocus()
                onKakaoLoginClick()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 구글 로그인 버튼 — 색은 회원가입 버튼(AfternoteButtonType.Plain)과 같은 gray2/gray9/gray3.
        SocialLoginButton(
            text = stringResource(R.string.onboarding_login_google),
            iconPainter = painterResource(com.afternote.core.ui.R.drawable.core_ui_img_google_logo),
            containerColor = AfternoteDesign.colors.gray2,
            contentColor = AfternoteDesign.colors.gray9,
            border = BorderStroke(1.dp, AfternoteDesign.colors.gray3),
            onClick = {
                focusManager.clearFocus()
                onGoogleLoginClick()
            },
        )

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

/** 카카오 로그인 버튼 배경 — 카카오 브랜드 고정색이라 앱 팔레트 토큰으로 근사하지 않는다. */
private val KakaoContainerColor = Color(0xFFFEE500)

/** 카카오 로그인 버튼 라벨 색 — 심볼 벡터(onboarding_ic_kakao_symbol)와 같은 #212121·알파 90.2%. */
private val KakaoContentColor = Color(0xE6212121)

/**
 * 소셜 로그인 버튼 공통 프레임 — 카카오·구글이 색과 아이콘만 다르고 같은 규격을 쓰도록 강제한다.
 *
 * 시안의 두 버튼은 출처가 달라(카카오 공식 에셋 이미지 vs 별도 컴포넌트) 셰이프·타이포·아이콘
 * 배치가 제각각이었고, 통일 판단은 FE 재량으로 확정된 건이다(#775). 규격은 이 화면의 다른
 * 버튼([AfternoteButton])과 동일한 6dp 셰이프·48dp 높이·captionLargeB 라벨로 수렴시킨다.
 * 아이콘은 장식이라 contentDescription 없이 라벨 [text] 가 접근성 이름을 담당한다.
 */
@Composable
private fun SocialLoginButton(
    text: String,
    iconPainter: Painter,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp),
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = AfternoteDesign.typography.captionLargeB,
            )
        }
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
