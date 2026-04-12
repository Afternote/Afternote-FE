package com.afternote.feature.onboarding.presentation.terms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.signup.scaffold.OnboardingScaffold

@Composable
fun TermsDetailScreen(
    title: String,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingScaffold(
        buttonText = stringResource(id = R.string.terms_next),
        onBackClick = onBackClick,
        onActionButtonClick = onNextClick,
        modifier = modifier,
        {
            val scrollState = rememberScrollState()

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.terms_detail_intro),
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray9,
                )
                // 인트로 텍스트
                Text(
                    text = stringResource(R.string.terms_detail_intro),
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray9,
                )

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
                TermsSectionText(
                    title = "4. 계정 및 데이터 관리",
                    content = stringResource(R.string.terms_detail_section_4),
                )
                TermsSectionText(
                    title = "5. 서비스 변경 및 종료",
                    content = stringResource(R.string.terms_detail_section_5),
                )
                TermsSectionText(
                    title = "6. 동의 철회",
                    content = stringResource(R.string.terms_detail_section_6),
                )

                // 맨 마지막 항목 아래에 약간의 여유 공간 추가 (스크롤 끝 도달 시 숨통 트이는 UX)
                Spacer(modifier = Modifier.height(106.dp))
            }
        },
    )
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
    Column(modifier = modifier) {
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
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsDetailScreenPreview() {
    AfternoteTheme {
        TermsDetailScreen(
            title = "서비스 이용약관",
            onBackClick = {},
            onNextClick = {},
        )
    }
}
