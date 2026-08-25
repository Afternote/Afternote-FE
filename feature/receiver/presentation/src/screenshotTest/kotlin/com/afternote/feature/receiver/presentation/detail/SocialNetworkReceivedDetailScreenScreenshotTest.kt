package com.afternote.feature.receiver.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * 자격증명 부재 — 서버가 수신자에게 `credentials` 를 내려주지 않는 실제 상태다.
 *
 * 예전에는 이 baseline 이 빈 아이디와 `••••••••` 마스킹을 찍었다. 가려진 값이 있다는 표시였지만
 * "표시" 를 눌러도 빈 값뿐이었다 (#619).
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun socialNetworkReceivedDetailScreenScreenshot() {
    AfternoteTheme {
        SocialNetworkReceivedDetailScreen(onBackClick = {})
    }
}

/** 자격증명이 온전히 전달된 상태 — 마스킹과 표시 토글이 붙는 유일한 경우다. */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun socialNetworkReceivedDetailScreenWithCredentialsScreenshot() {
    AfternoteTheme {
        SocialNetworkReceivedDetailScreen(
            onBackClick = {},
            content =
                ReceivedSocialNetworkDetailContent(
                    serviceName = "인스타그램",
                    credentials =
                        ReceivedAccountCredentialsUiModel(
                            accountId = "qwerty123",
                            password = "qwerty123!",
                        ),
                ),
        )
    }
}

/** 발신자가 아이디만 남긴 상태 — 비밀번호 자리에 마스킹 대신 부재를 명시한다. */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun socialNetworkReceivedDetailScreenWithIdOnlyScreenshot() {
    AfternoteTheme {
        SocialNetworkReceivedDetailScreen(
            onBackClick = {},
            content =
                ReceivedSocialNetworkDetailContent(
                    serviceName = "인스타그램",
                    credentials =
                        ReceivedAccountCredentialsUiModel(
                            accountId = "qwerty123",
                            password = null,
                        ),
                ),
        )
    }
}
