package com.afternote.feature.onboarding.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
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
fun BottomButtons(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 간편 회원가입하기 버튼
        BottomButton(
            text = "간편 회원가입하기",
            backgroundColor = AfternoteDesign.colors.gray2,
            textColor = Color.Black,
            onClick = { /* 회원가입 로직 */ },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 카카오 로그인 버튼
        Image(
            modifier =
                Modifier
                    .height(52.dp)
                    .clickable(onClick = { /* 카카오 로그인 로직 */ }),
            painter = painterResource(R.drawable.kakao_login_large_wide_1),
            contentDescription = "카카오 로그인 버튼",
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 아이디/비밀번호 찾기
        Text(
            text = "아이디/비밀번호 찾기",
            style =
                AfternoteDesign.typography.captionLargeR.copy(
                    color = AfternoteDesign.colors.gray6,
                    textDecoration = TextDecoration.Underline, // 밑줄 구현
                ),
            modifier = Modifier.clickable { /* 찾기 로직 */ },
        )
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
