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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.feedback.InfoPopup
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.R

@Composable
fun LoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = AfternoteDesign.colors.gray9,
                contentColor = AfternoteDesign.colors.white,
                disabledContainerColor = AfternoteDesign.colors.gray9,
                disabledContentColor = AfternoteDesign.colors.white,
            ),
    ) {
        Text(
            text = text,
            style =
                AfternoteDesign.typography.bodyBase.copy(
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}

@Composable
fun BottomButtons(
    onSignUpClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFindAccountPopup by remember { mutableStateOf(false) }

    if (showFindAccountPopup) {
        InfoPopup(
            message = "아이디/비밀번호 찾기의 경우,\n고객센터로 문의 바랍니다.",
            onConfirm = { showFindAccountPopup = false },
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 간편 회원가입하기 버튼
        BottomButton(
            text = "간편 회원가입하기",
            backgroundColor = AfternoteDesign.colors.gray2,
            textColor = Color.Black,
            onClick = onSignUpClick,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 카카오 로그인 버튼
        Button(
            onClick = onKakaoLoginClick,
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
                contentDescription = "카카오 로그인",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillBounds,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 아이디/비밀번호 찾기
        TextButton(
            onClick = { showFindAccountPopup = true },
        ) {
            Text(
                text = "아이디/비밀번호 찾기",
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        color = AfternoteDesign.colors.gray6,
                        textDecoration = TextDecoration.Underline,
                    ),
            )
        }
    }
}

@Composable
fun BottomButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = textColor,
            ),
    ) {
        Text(text = text, style = AfternoteDesign.typography.bodySmallR)
    }
}
