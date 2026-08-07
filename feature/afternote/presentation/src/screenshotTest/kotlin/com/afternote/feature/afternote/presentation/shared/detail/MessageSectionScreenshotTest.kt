package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.shared.model.MessageBlockUiModel
import com.android.tools.screenshot.PreviewTest

/**
 * 남기신 말씀이 여러 개인 상태 — 시안대로 블록마다 카드가 하나씩 생긴다 (이슈 #509).
 * 상세 화면 스크린샷들은 기본값(빈 콘텐츠)으로 렌더해 이 상태를 덮지 못하므로 여기서 직접 잡는다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun messageSectionBlocksScreenshot() {
    AfternoteTheme {
        MessageSection(
            blocks =
                listOf(
                    MessageBlockUiModel(
                        title = "가족에게",
                        body = "이 계정에는 우리 가족 여행 사진이 많아.\n계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
                    ),
                    MessageBlockUiModel(body = "비밀번호는 주기적으로 바뀌니 메모 앱도 함께 확인해 줘."),
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun messageSectionEmptyScreenshot() {
    AfternoteTheme {
        MessageSection(
            blocks = emptyList(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
