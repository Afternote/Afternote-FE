package com.afternote.feature.onboarding.presentation.login.component

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.afternote.core.ui.addFocusCleaner
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    emailState: TextFieldState,
    passwordState: TextFieldState,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.login_top_bar_title),
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
                        .padding(horizontal = 24.dp, vertical = 45.dp),
            ) {
                SocialLoginGroup(
                    onSignUpClick = onSignUpClick,
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
                    .imePadding()
                    .addFocusCleaner(focusManager)
                    .padding(horizontal = 24.dp),
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
                    placeholder = stringResource(R.string.login_email_label),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )

                // 비밀번호 입력 필드
                AfternoteTextField(
                    state = passwordState,
                    modifier =
                        Modifier.semantics { contentType = ContentType.Password },
                    placeholder = stringResource(R.string.login_password_label),
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 로그인 버튼
            AfternoteButton(
                text = stringResource(R.string.login_button),
                onClick = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                type = AfternoteButtonType.Default,
            )
        }
    }
}

/**
 * 로그인 화면 하단 소셜 로그인 그룹.
 *
 * `LoginScreen`에서만 쓰이므로 외부 파일로 분리하지 않고 `private`로 캡슐화한다.
 * `showFindAccountPopup` 상태를 내부에 가두어 부모 화면의 리컴포지션 범위를 줄인다.
 */
@Composable
private fun SocialLoginGroup(
    onSignUpClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var showFindAccountPopup by remember { mutableStateOf(false) }

    if (showFindAccountPopup) {
        Popup(
            type = PopupType.Default,
            message = stringResource(R.string.login_find_account_message),
            onConfirm = { showFindAccountPopup = false },
            onDismiss = { showFindAccountPopup = false },
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 간편 회원가입하기 버튼
        AfternoteButton(
            text = stringResource(R.string.login_signup_simple),
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
                painter = painterResource(R.drawable.kakao_login_large_wide_1),
                contentDescription = stringResource(R.string.login_kakao),
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
                    // 💡 주의: 0xF2F2F2 앞에 투명도(FF)를 붙여야 색상이 정상적으로 보입니다.
                    containerColor = Color(0xFFF2F2F2),
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
                    contentDescription = "구글 로그인",
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = stringResource(R.string.login_google),
                style = AfternoteDesign.typography.captionLargeB,
                color = AfternoteDesign.colors.gray8,
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 아이디/비밀번호 찾기
        Text(
            text = stringResource(R.string.login_find_account),
            style =
                AfternoteDesign.typography.captionLargeR.copy(
                    color = AfternoteDesign.colors.gray6,
                    textDecoration = TextDecoration.Underline,
                ),
            modifier =
                Modifier.clickable {
                    focusManager.clearFocus()
                    showFindAccountPopup = true
                },
        )
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
            onGoogleLoginClick = {},
            onBackClick = {},
        )
    }
}
