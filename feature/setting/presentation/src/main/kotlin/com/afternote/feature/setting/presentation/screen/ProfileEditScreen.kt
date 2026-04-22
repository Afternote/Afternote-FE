package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "프로필 수정",
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        val nameState = rememberTextFieldState()
        val phoneState = rememberTextFieldState()
        val emailState = rememberTextFieldState()
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding), // ← 이게 없으면 TopAppBar 뒤로 content가 숨어버림
        ) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp),
                    contentAlignment = Alignment.Center, // 수평 중앙 정렬
                ) {
                    Box(
                        modifier = Modifier.size(134.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_default_profile),
                            contentDescription = "기본",
                            modifier = Modifier.fillMaxSize(),
                        )
                        Image(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = "추가",
                            modifier =
                                Modifier
                                    .size(80.dp)
                                    .align(Alignment.BottomEnd),
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.padding(top = 58.dp))
            }
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = "이름",
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    AfternoteTextField(
                        state = nameState,
                        placeholder = "이름을 지정해주세요",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "연락처",
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    AfternoteTextField(
                        state = phoneState,
                        placeholder = "연락처를 지정해주세요",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "이메일",
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    AfternoteTextField(
                        state = emailState,
                        placeholder = "이메일을 지정해주세요",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                AfternoteButton(
                    text = "수정하기",
                    onClick = { },
                    type = AfternoteButtonType.Default,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                )
            }
            item{
                Text("회원탈퇴하기")

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenPrev() {
    ProfileEditScreen(onBackClick = {})
}
