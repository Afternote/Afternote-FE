package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * 자격증명 부재 — 서버가 수신자에게 `credentials` 를 내려주지 않는 실제 상태다.
 *
 * 예전에는 이 baseline 이 빈 아이디와 `••••••••` 마스킹을 찍었다. 가려진 값이 있다는 표시였지만
 * "표시" 를 눌러도 빈 값뿐이었다 (#619).
 *
 * 세 baseline 은 **같은 애프터노트의 자격증명 상태만 다른 판**이다 — 서비스명을 함께 채우는 이유는
 * 제목이 빈 화면이 실재하지 않기 때문이다. 제목은 에디터 검증(`TITLE_REQUIRED`)·서버
 * `@NotBlank`·DB `nullable = false` 로 3중으로 강제된다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun socialNetworkReceivedDetailScreenScreenshot() {
    AfternoteTheme {
        SocialNetworkReceivedDetailScreen(
            onBackClick = {},
            content = ReceivedSocialNetworkDetailContent(serviceName = "인스타그램"),
        )
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
