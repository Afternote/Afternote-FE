package com.afternote.feature.afternote.presentation.shared.fingerprint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.scaffold.bottombar.BottomBar
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.nav.ui.navgraph.AfternoteLightTheme
import com.afternote.feature.afternote.presentation.shared.shell.TopBar

/**
 * 지문 로그인 화면
 *
 * 피그마 디자인 기반:
 * - 헤더: "지문 로그인" 타이틀
 * - 안내 텍스트: "사용자 인증 후 조회가 가능합니다."
 * - 지문 아이콘 (중앙)
 * - 버튼: "지문 인증하기" (B3 배경색)
 * - 하단 네비게이션 바
 */
@Composable
@Suppress("AssignedValueIsNeverRead")
fun FingerprintLoginScreen(
    modifier: Modifier = Modifier,
    onFingerprintAuthClick: () -> Unit = {},
) {
    var selectedNavTab by remember { mutableStateOf(BottomNavTab.NOTE) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(title = "지문 로그인")
        },
        bottomBar = {
            BottomBar(
                selectedNavTab = selectedNavTab,
                onTabClick = TODO(),
                modifier = TODO()
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // 메인 컨텐츠
            FingerprintAuthContent(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                onFingerprintAuthClick = onFingerprintAuthClick,
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=420,isRound=false",
)
@Composable
private fun FingerprintLoginScreenPreview() {
    AfternoteLightTheme {
        FingerprintLoginScreen()
    }
}
