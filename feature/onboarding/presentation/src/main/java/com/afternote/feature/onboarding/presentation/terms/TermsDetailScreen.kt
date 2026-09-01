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
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.R

@Composable
fun TermsDetailScreen(
    title: String,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowStepScaffold(
        topBarTitle = stringResource(R.string.onboarding_signup_title),
        actionButtonText = stringResource(id = R.string.onboarding_terms_next),
        onBackClick = onBackClick,
        onActionClick = onNextClick,
        modifier = modifier,
        content = {
            val scrollState = rememberScrollState()

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 인트로 텍스트
                Text(
                    text = stringResource(R.string.onboarding_terms_detail_intro),
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray9,
                )

                // 각 약관 섹션들 (반복되는 구조는 별도 컴포저블로 분리하여 가독성 확보)
                TermsSectionText(
                    title = stringResource(R.string.onboarding_terms_detail_section_1_title),
                    content = stringResource(R.string.onboarding_terms_detail_section_1),
                )
                TermsSectionText(
                    title = stringResource(R.string.onboarding_terms_detail_section_2_title),
                    content = stringResource(R.string.onboarding_terms_detail_section_2),
                )
                TermsSectionText(
                    title = stringResource(R.string.onboarding_terms_detail_section_3_title),
                    content = stringResource(R.string.onboarding_terms_detail_section_3),
                )
                TermsSectionText(
                    title = stringResource(R.string.onboarding_terms_detail_section_4_title),
                    content = stringResource(R.string.onboarding_terms_detail_section_4),
                )
                TermsSectionText(
                    title = stringResource(R.string.onboarding_terms_detail_section_5_title),
                    content = stringResource(R.string.onboarding_terms_detail_section_5),
                )
                TermsSectionText(
                    title = stringResource(R.string.onboarding_terms_detail_section_6_title),
                    content = stringResource(R.string.onboarding_terms_detail_section_6),
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
