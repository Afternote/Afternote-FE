package com.afternote.feature.onboarding.presentation.terms

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.CircleCheckBox
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.scaffold.ProgressBarScaffold
import com.afternote.core.common.R as CommonR

enum class TermsType {
    SERVICE,
    PRIVACY,
    MARKETING,
}

@Immutable
data class TermsState(
    val isTermsAgreed: Boolean = false,
    val isPrivacyAgreed: Boolean = false,
    val isMarketingAgreed: Boolean = false,
) {
    val isAllAgreed: Boolean get() = isTermsAgreed && isPrivacyAgreed && isMarketingAgreed
}

@Composable
fun OnboardingTermsScreen(
    termsState: TermsState,
    onTermsToggle: (Boolean) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onMarketingToggle: (Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onViewTermsClick: (TermsType) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProgressBarScaffold(
        currentStep = 4,
        onBackClick = onBackClick,
        onNextClick = onNextClick,
        modifier = modifier,
        {
            Spacer(modifier = Modifier.height(32.dp))

            // 로고
            Image(
                painter = painterResource(CommonR.drawable.core_common_logo),
                contentDescription = stringResource(R.string.welcome_logo_description),
                modifier = Modifier.height(55.dp),
                contentScale = ContentScale.FillHeight,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 환영 텍스트
            Text(
                text = stringResource(R.string.terms_welcome),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.terms_description),
                style =
                    AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- 약관 동의 섹션 ---

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TermsRow(
                    title = stringResource(R.string.terms_agree_all),
                    isChecked = termsState.isAllAgreed,
                    onToggle = { onToggleAll(!termsState.isAllAgreed) },
                    titleStyle = AfternoteDesign.typography.bodyBase,
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = AfternoteDesign.colors.gray3,
                )

                // 서비스 이용 약관 (필수)
                TermsRow(
                    title = stringResource(R.string.terms_service),
                    isChecked = termsState.isTermsAgreed,
                    onToggle = { onTermsToggle(!termsState.isTermsAgreed) },
                    titleStyle = AfternoteDesign.typography.bodySmallB,
                ) { onViewTermsClick(TermsType.SERVICE) }

                // 개인정보 수집 및 이용 동의서 (필수)
                TermsRow(
                    title = stringResource(R.string.terms_privacy),
                    isChecked = termsState.isPrivacyAgreed,
                    onToggle = { onPrivacyToggle(!termsState.isPrivacyAgreed) },
                    titleStyle = AfternoteDesign.typography.bodySmallB,
                ) { onViewTermsClick(TermsType.PRIVACY) }

                // 마케팅 수신 동의 (선택)
                TermsRow(
                    title = stringResource(R.string.terms_marketing),
                    isChecked = termsState.isMarketingAgreed,
                    onToggle = { onMarketingToggle(!termsState.isMarketingAgreed) },
                    isOptional = true,
                    titleStyle = AfternoteDesign.typography.bodySmallB,
                ) { onViewTermsClick(TermsType.MARKETING) }
            }
            Spacer(Modifier.height(71.dp))
        },
    )
}

@Composable
private fun TermsRow(
    title: String,
    isChecked: Boolean,
    titleStyle: TextStyle,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isOptional: Boolean = false,
    onViewDetailClick: (() -> Unit)? = null,
) {
    // 최상위 Row는 레이아웃 배치만 담당 (이벤트 없음)
    Row(
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        // 1. 체크박스 + 텍스트 영역 (Toggleable)
        Row(
            modifier =
                Modifier
                    .toggleable(
                        value = isChecked,
                        role = Role.Checkbox,
                        onValueChange = { onToggle() },
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleCheckBox(
                checked = isChecked,
                onCheckedChange = null,
            )

            Spacer(Modifier.width(8.dp))

            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    color = AfternoteDesign.colors.gray9,
                )

                if (isOptional) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.terms_marketing_description),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray4,
                    )
                }
            }
        }

        // 2. 전체보기 버튼 (독립된 Clickable — toggleable과 이벤트 분리)
        if (onViewDetailClick != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.terms_view_detail),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
                modifier = Modifier.clickable(role = Role.Button, onClick = onViewDetailClick),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingTermsScreenPreview() {
    var state by remember { mutableStateOf(TermsState()) }

    AfternoteTheme {
        OnboardingTermsScreen(
            termsState = state,
            onTermsToggle = { state = state.copy(isTermsAgreed = it) },
            onPrivacyToggle = { state = state.copy(isPrivacyAgreed = it) },
            onMarketingToggle = { state = state.copy(isMarketingAgreed = it) },
            onToggleAll = { isChecked ->
                state =
                    state.copy(
                        isTermsAgreed = isChecked,
                        isPrivacyAgreed = isChecked,
                        isMarketingAgreed = isChecked,
                    )
            },
            onViewTermsClick = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
