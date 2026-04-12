package com.afternote.feature.onboarding.presentation.terms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsDetailScreen(
    title: String,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 1. Scaffold를 통해 상단 바, 본문, 하단 고정 바의 레이아웃을 완벽히 분리
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AfternoteDesign.colors.white,
        topBar = {
            DetailTopBar(
                title = "회원가입", // TODO:stringResource로 빼
                onBackClick = {
                    onBackClick()
                },
            )
        },
        bottomBar = {
            val navBarHeight =
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val exactBottomPadding = max(navBarHeight, 49.dp)
            AfternoteButton(
                text = "프로필 설정하러가기",
                onClick = {
                    onNextClick()
                },
                type =
                    AfternoteButtonType.Default,
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = exactBottomPadding),
            )
        },
    ) { paddingValues ->
        // 🔥 이 paddingValues 안에 bottomBar의 높이가 계산되어 들어옵니다.

        // 3. 스크롤 가능한 본문 영역
        val scrollState = rememberScrollState()

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // 🔥 상단바 및 하단바 높이만큼의 패딩 적용 (텍스트 가림 방지)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp), // 본문 좌우 여백
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 인트로 텍스트
            Text(
                text = stringResource(R.string.terms_detail_intro),
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray8,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 각 약관 섹션들 (반복되는 구조는 별도 컴포저블로 분리하여 가독성 확보)
            TermsSectionText(
                title = "1. 서비스 이용약관",
                content = stringResource(R.string.terms_detail_section_1),
            )
            TermsSectionText(
                title = "2. 개인정보 수집 및 이용 동의",
                content = stringResource(R.string.terms_detail_section_2),
            )
            TermsSectionText(
                title = "3. 기록 및 콘텐츠 저장에 대한 안내",
                content = stringResource(R.string.terms_detail_section_3),
            )
            // ... (나머지 섹션들)

            // 맨 마지막 항목 아래에 약간의 여유 공간 추가 (스크롤 끝 도달 시 숨통 트이는 UX)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 반복되는 약관 제목 + 내용을 그리는 도우미 컴포저블
 */
@Composable
private fun TermsSectionText(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 32.dp)) {
        // 섹션 간 간격 (고정 dp 사용 구간)
        Text(
            text = title,
            style = AfternoteDesign.typography.h3,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = content,
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray7,
        )
    }
}

// (참고용) 공통 버튼 컴포넌트 Mock
@Composable
private fun AfternotePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 기존 구현된 블랙 색상의 프로필 설정하러가기 버튼
}
