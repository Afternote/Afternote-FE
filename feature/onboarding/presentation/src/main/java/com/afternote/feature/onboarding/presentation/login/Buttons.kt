package com.afternote.feature.onboarding.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.popup.InfoPopup
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.R

enum class LoginButtonStyle {
    /** gray9 배경 (로그인) */
    Primary,

    /** gray2 + 테두리 스타일 (간편 회원가입) */
    Secondary,
}

@Composable
fun LoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: LoginButtonStyle = LoginButtonStyle.Primary,
) {
    val type =
        when {
            !enabled -> AfternoteButtonType.Un
            style == LoginButtonStyle.Secondary -> AfternoteButtonType.Plain
            else -> AfternoteButtonType.Default
        }
    AfternoteButton(
        text = text,
        onClick = onClick,
        modifier = modifier.height(48.dp),
        type = type,
    )
}

@Composable
fun BottomButtons(
    onSignUpClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var showFindAccountPopup by remember { mutableStateOf(false) }

    if (showFindAccountPopup) {
        InfoPopup(
            message = stringResource(R.string.login_find_account_message),
            onConfirm = { showFindAccountPopup = false },
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 간편 회원가입하기 버튼
        LoginButton(
            text = stringResource(R.string.login_signup_simple),
            style = LoginButtonStyle.Secondary,
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

        Spacer(modifier = Modifier.height(24.dp))

        // 아이디/비밀번호 찾기
        TextButton(
            onClick = {
                focusManager.clearFocus()
                showFindAccountPopup = true
            },
        ) {
            Text(
                text = stringResource(R.string.login_find_account),
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        color = AfternoteDesign.colors.gray6,
                        textDecoration = TextDecoration.Underline,
                    ),
            )
        }
    }
}
