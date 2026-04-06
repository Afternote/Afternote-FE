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
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.AfternoteEmbeddedMainBottomBar
import com.afternote.feature.afternote.presentation.shared.AfternoteTopBar

/**
 * 지문 로그인 화면
 *
 * 피그마 디자인 기반:
 * - 헤더: "지문 로그인" 타이틀
 * - 안내 텍스트: "사용자 인증 후 조회가 가능합니다."
 * - 지문 아이콘 (중앙)
 * - 버튼: "지문 인증하기" (Gray9 배경색)
 * - 하단 네비게이션 바
 */
@Composable
@Suppress("AssignedValueIsNeverRead")
fun FingerprintLoginScreen(
    modifier: Modifier = Modifier,
    onFingerprintAuthClick: () -> Unit = {},
    onNavTabSelected: (BottomNavTab) -> Unit,
) {
    var selectedNavTab by remember { mutableStateOf(BottomNavTab.NOTE) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AfternoteTopBar(title = "지문 로그인")
        },
        bottomBar = {
            AfternoteEmbeddedMainBottomBar(
                selectedNavTab = selectedNavTab,
                onTabClick = onNavTabSelected,
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
