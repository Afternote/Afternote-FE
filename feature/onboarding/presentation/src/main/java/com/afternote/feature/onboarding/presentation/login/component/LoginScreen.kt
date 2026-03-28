package com.afternote.feature.onboarding.presentation.login.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.MainButton
import com.afternote.core.ui.TopBar
import com.afternote.core.ui.theme.White

// TODO:AI 딸깍하기만 하고 아무 것도 안 함

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    // 입력 필드의 상태를 관리합니다.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopBar()
        },
        containerColor = White, // 배경색을 흰색으로 설정
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            // 좌우 여백 설정
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 아이디 (이메일) 입력 필드
            MyInputField(
                label = "아이디 (이메일)",
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 비밀번호 입력 필드
            MyInputField(
                label = "비밀번호",
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                isPassword = true,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 로그인 버튼
            MainButton(
                text = "로그인",
                onClick = { /* 로그인 로직 */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotEmpty() && password.isNotEmpty(), // 입력된 내용이 있을 때만 활성화
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 하단 버튼 영역
            BottomButtons()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen()
    }
}
