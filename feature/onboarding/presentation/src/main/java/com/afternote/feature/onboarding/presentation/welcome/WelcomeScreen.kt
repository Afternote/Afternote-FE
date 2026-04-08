package com.afternote.feature.onboarding.presentation.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.addFocusCleaner
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.noRippleClickable
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.core.common.R as CommonR

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    onCheckRecordsClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .addFocusCleaner(focusManager)
                .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
        ) {
            // 로고
            Image(
                painter = painterResource(CommonR.drawable.core_common_logo),
                contentDescription = stringResource(R.string.welcome_logo_description),
                modifier = Modifier.size(width = 160.dp, height = 60.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 메인 타이틀
            Text(
                text = stringResource(R.string.welcome_title),
                style = AfternoteDesign.typography.h2,
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 설명 텍스트
            Text(
                text = stringResource(R.string.welcome_description),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray5,
            )
        }

        // 하단 버튼 영역
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 시작하기 버튼
            AfternoteButton(
                text = stringResource(R.string.welcome_start),
                onClick = {
                    focusManager.clearFocus()
                    onStartClick()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                type = AfternoteButtonType.Default,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 전달 받은 기록 확인하기 버튼
            AfternoteButton(
                text = stringResource(R.string.welcome_check_records),
                onClick = {
                    focusManager.clearFocus()
                    onCheckRecordsClick()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                type = AfternoteButtonType.Plain,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 이미 가입하셨나요? 로그인하기
            Row {
                Text(
                    text = stringResource(R.string.welcome_already_signed_up),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray5,
                )
                Text(
                    text = stringResource(R.string.welcome_login),
                    style =
                        AfternoteDesign.typography.captionLargeR.copy(
                            textDecoration = TextDecoration.Underline,
                        ),
                    color = AfternoteDesign.colors.gray6,
                    modifier =
                        Modifier.noRippleClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                focusManager.clearFocus()
                                onLoginClick()
                            },
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomeScreenPreview() {
    AfternoteTheme {
        WelcomeScreen(
            onStartClick = {},
            onCheckRecordsClick = {},
            onLoginClick = {},
        )
    }
}
