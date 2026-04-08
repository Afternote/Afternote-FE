package com.afternote.feature.onboarding.presentation.terms

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.signup.SIGN_UP_TOTAL_STEPS
import com.afternote.core.common.R as CommonR
import com.afternote.core.ui.R as UiR

@Immutable
data class TermsState(
    val isTermsAgreed: Boolean = false,
    val isPrivacyAgreed: Boolean = false,
    val isMarketingAgreed: Boolean = false,
) {
    val isAllAgreed: Boolean get() = isTermsAgreed && isPrivacyAgreed && isMarketingAgreed
    val isNextEnabled: Boolean get() = isTermsAgreed && isPrivacyAgreed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingTermsScreen(
    currentStep: Int,
    termsState: TermsState,
    onTermsToggle: (Boolean) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onMarketingToggle: (Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onViewTermsClick: (Int) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressDescription =
        stringResource(R.string.terms_progress_description, currentStep, SIGN_UP_TOTAL_STEPS)

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.terms_top_bar_title),
                onBackClick = onBackClick,
            )
        },
        containerColor = AfternoteDesign.colors.white,
    ) { innerPadding ->
        OnboardingTermsContent(
            currentStep = currentStep,
            termsState = termsState,
            onTermsToggle = onTermsToggle,
            onPrivacyToggle = onPrivacyToggle,
            onMarketingToggle = onMarketingToggle,
            onToggleAll = onToggleAll,
            onViewTermsClick = onViewTermsClick,
            onNextClick = onNextClick,
            progressDescription = progressDescription,
            modifier =
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
        )
    }
}

@Composable
private fun OnboardingTermsContent(
    currentStep: Int,
    termsState: TermsState,
    onTermsToggle: (Boolean) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onMarketingToggle: (Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onViewTermsClick: (Int) -> Unit,
    onNextClick: () -> Unit,
    progressDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // 진행바
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / SIGN_UP_TOTAL_STEPS },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .semantics { contentDescription = progressDescription },
            color = AfternoteDesign.colors.gray9,
            trackColor = AfternoteDesign.colors.gray3,
            strokeCap = StrokeCap.Square,
        )

        // 메인 콘텐츠 (스크롤 가능)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 로고
            Image(
                painter = painterResource(CommonR.drawable.core_common_logo),
                contentDescription = stringResource(R.string.welcome_logo_description),
                modifier = Modifier.size(width = 160.dp, height = 60.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 환영 텍스트
            Text(
                text = stringResource(R.string.terms_welcome),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.terms_description),
                style =
                    AfternoteDesign.typography.bodyLargeR.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(80.dp))

            // --- 약관 동의 섹션 ---

            // 전체 동의
            TermsRow(
                title = stringResource(R.string.terms_agree_all),
                isChecked = termsState.isAllAgreed,
                onToggle = { onToggleAll(!termsState.isAllAgreed) },
                titleStyle = TermsRowTitleStyle.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AfternoteDesign.colors.gray3, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 서비스 이용 약관 (필수)
            TermsRow(
                title = stringResource(R.string.terms_service),
                isChecked = termsState.isTermsAgreed,
                onToggle = { onTermsToggle(!termsState.isTermsAgreed) },
                onViewDetailClick = { onViewTermsClick(1) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 개인정보 수집 및 이용 동의서 (필수)
            TermsRow(
                title = stringResource(R.string.terms_privacy),
                isChecked = termsState.isPrivacyAgreed,
                onToggle = { onPrivacyToggle(!termsState.isPrivacyAgreed) },
                onViewDetailClick = { onViewTermsClick(2) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 마케팅 수신 동의 (선택)
            TermsRow(
                title = stringResource(R.string.terms_marketing),
                subtitle = stringResource(R.string.terms_marketing_description),
                isChecked = termsState.isMarketingAgreed,
                onToggle = { onMarketingToggle(!termsState.isMarketingAgreed) },
                onViewDetailClick = { onViewTermsClick(3) },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 하단 고정 버튼
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Button(
                onClick = onNextClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = AfternoteDesign.colors.gray9,
                        contentColor = AfternoteDesign.colors.white,
                        disabledContainerColor = AfternoteDesign.colors.gray4,
                        disabledContentColor = AfternoteDesign.colors.white,
                    ),
                enabled = termsState.isNextEnabled,
            ) {
                Text(
                    text = stringResource(R.string.terms_next),
                    style = AfternoteDesign.typography.primaryButton,
                )
            }
        }
    }
}

private enum class TermsRowTitleStyle {
    Normal,
    Bold,
}

@Composable
private fun TermsRow(
    title: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: TermsRowTitleStyle = TermsRowTitleStyle.Normal,
    onViewDetailClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .toggleable(
                    value = isChecked,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ).padding(vertical = 4.dp),
        verticalAlignment =
            if (subtitle != null) Alignment.Top else Alignment.CenterVertically,
    ) {
        // 원형 체크 토글
        Icon(
            painter =
                painterResource(
                    if (isChecked) UiR.drawable.core_ui_check_circle else UiR.drawable.core_ui_uncheck_circle,
                ),
            contentDescription = null,
            tint =
                if (isChecked) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray4,
            modifier =
                Modifier
                    .padding(top = if (subtitle != null) 2.dp else 0.dp)
                    .size(24.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style =
                    when (titleStyle) {
                        TermsRowTitleStyle.Bold ->
                            AfternoteDesign.typography.bodyLargeB

                        TermsRowTitleStyle.Normal ->
                            AfternoteDesign.typography.bodyBase.copy(
                                fontWeight = FontWeight.Medium,
                            )
                    },
                color = AfternoteDesign.colors.gray9,
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray4,
                )
            }
        }

        if (onViewDetailClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.terms_view_detail),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray4,
                modifier =
                    Modifier
                        .clickable(role = Role.Button, onClick = onViewDetailClick)
                        .padding(4.dp),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingTermsScreenPreview() {
    var state by remember { mutableStateOf(TermsState()) }

    AfternoteTheme {
        OnboardingTermsContent(
            currentStep = 1,
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
            progressDescription = "회원가입 진행도 4단계 중 1단계",
        )
    }
}
