package com.afternote.feature.onboarding.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.modifierextention.noRippleClickable
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
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
                .navigationBarsPadding()
                .addFocusCleaner(focusManager)
                .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        vertical = 48.dp,
                    ),
        ) {
            // 로고
            Image(
                painter = painterResource(CommonR.drawable.core_common_logo),
                contentDescription = stringResource(R.string.welcome_logo_description),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            end = 59.dp,
                        ),
                contentScale = ContentScale.FillWidth,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 메인 타이틀
            Text(
                text = stringResource(R.string.welcome_title),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.black,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 설명 텍스트 ("애프터노트"만 강조)
            val descriptionText = stringResource(R.string.welcome_description)
            val highlight = stringResource(R.string.welcome_brand_name)
            Text(
                text =
                    buildAnnotatedString {
                        val start = descriptionText.indexOf(highlight)
                        if (start >= 0) {
                            append(descriptionText.substring(0, start))
                            withStyle(
                                style = SpanStyle(color = AfternoteDesign.colors.gray9),
                            ) {
                                append(highlight)
                            }
                            append(descriptionText.substring(start + highlight.length))
                        } else {
                            append(descriptionText)
                        }
                    },
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray5,
            )
            Spacer(Modifier.weight(1f))
            // 하단 버튼 영역

            // 시작하기 버튼
            AfternoteButton(
                text = stringResource(R.string.welcome_start),
                onClick = {
                    focusManager.clearFocus()
                    onStartClick()
                },
                modifier =
                    Modifier
                        .fillMaxWidth(),
                type = AfternoteButtonType.Default,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 전달 받은 기록 확인하기 버튼
            AfternoteButton(
                text = stringResource(R.string.welcome_check_records),
                onClick = {
                    focusManager.clearFocus()
                    onCheckRecordsClick()
                },
                modifier =
                    Modifier
                        .fillMaxWidth(),
                type = AfternoteButtonType.Plain,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 이미 가입하셨나요? 로그인하기
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
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

@Preview(showBackground = true)
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
