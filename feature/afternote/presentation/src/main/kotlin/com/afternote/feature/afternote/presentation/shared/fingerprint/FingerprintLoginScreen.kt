package com.afternote.feature.afternote.presentation.shared.fingerprint

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 지문 로그인 화면
 *
 * 피그마 디자인 기반:
 * - 헤더: "지문 로그인" 타이틀
 * - 안내 텍스트: "사용자 인증 후 조회가 가능합니다."
 * - 지문 아이콘 (중앙)
 * - 버튼: "지문 인증하기" (AfternoteDesign.colors.gray9 배경색)
 */
@Composable
fun FingerprintLoginScreen(
    modifier: Modifier = Modifier,
    onFingerprintAuthClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DetailTopBar(title = "지문 로그인")
        },
    ) { paddingValues ->
        // 메인 컨텐츠
        FingerprintAuthContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            onFingerprintAuthClick = onFingerprintAuthClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FingerprintLoginScreenPreview() {
    AfternoteTheme {
        FingerprintLoginScreen(
            onFingerprintAuthClick = {},
        )
    }
}
