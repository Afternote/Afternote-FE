package com.afternote.feature.receiver.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.AfternoteDetailServiceHeader
import com.afternote.feature.afternote.presentation.shared.detail.DetailInfoRow
import com.afternote.feature.afternote.presentation.shared.detail.DetailSection
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection
import com.afternote.feature.afternote.presentation.shared.model.AfternoteServiceDisplay
import com.afternote.feature.afternote.presentation.shared.model.MessageBlockUiModel

/**
 * 수신 소셜 네트워크 상세 (Stateless).
 *
 * 발신자 [com.afternote.feature.afternote.presentation.author.detail.account.AccountDetailScreen]
 * 과 동일한 Scaffold/스크롤 패턴을 따르되, TopBar 우측 편집/삭제 액션을 두지 않는다.
 */
@Composable
fun SocialNetworkReceivedDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: ReceivedSocialNetworkDetailContent = ReceivedSocialNetworkDetailContent(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.feature_afternote_detail_title),
                onBackClick = onBackClick,
            )
        },
    ) { paddingValues ->
        SocialNetworkReceivedDetailScrollContent(
            content = content,
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        )
    }
}

@Composable
private fun SocialNetworkReceivedDetailScrollContent(
    content: ReceivedSocialNetworkDetailContent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp)
                .padding(horizontal = 20.dp),
    ) {
        AfternoteDetailServiceHeader(
            service =
                AfternoteServiceDisplay.fromService(
                    serviceName = content.serviceName,
                    type = AfternoteType.SOCIAL_NETWORK,
                ),
            finalWriteDate = content.finalWriteDate,
        )

        Spacer(modifier = Modifier.height(31.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ReceivedAccountSection(
                accountId = content.accountId,
                password = content.password,
            )
            ProcessingMethodsSection(methods = content.processingMethods)
            MessageSection(blocks = content.messageBlocks)
        }
    }
}

/**
 * ACCOUNT(아이디·비밀번호) 섹션. 비밀번호 표시 토글만 보유.
 *
 * 발신자 화면의 동명 private composable과 시각적으로 동일하나, 향후 노출 정책 변경 가능성에 대비해
 * receiver 화면에 별도 정의한다.
 */
@Composable
private fun ReceivedAccountSection(
    accountId: String,
    password: String,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    DetailSection(
        iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
        label = stringResource(R.string.feature_afternote_detail_section_account),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailInfoRow(
                iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
                label = stringResource(R.string.feature_afternote_detail_label_id),
                value = accountId,
            )
            HorizontalDivider(
                color = AfternoteDesign.colors.gray2,
                thickness = 1.dp,
            )
            DetailInfoRow(
                iconResId = R.drawable.feature_afternote_ic_lock,
                label = stringResource(R.string.feature_afternote_detail_label_password),
                value =
                    if (passwordVisible) {
                        password
                    } else {
                        stringResource(R.string.feature_afternote_detail_password_mask)
                    },
                trailingContent = {
                    val toggleLabel =
                        if (passwordVisible) {
                            stringResource(R.string.feature_afternote_detail_password_hide)
                        } else {
                            stringResource(R.string.feature_afternote_detail_password_show)
                        }
                    Text(
                        text = toggleLabel,
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.b1,
                        modifier =
                            Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = toggleLabel,
                            ) {
                                passwordVisible = !passwordVisible
                            },
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SocialNetworkReceivedDetailScreenPreview() {
    AfternoteTheme {
        SocialNetworkReceivedDetailScreen(
            onBackClick = {},
            content =
                ReceivedSocialNetworkDetailContent(
                    serviceName = "인스타그램",
                    accountId = "qwerty123",
                    password = "qwerty123!",
                    processingMethods = listOf("게시물 내리기", "추모 게시물 올리기", "추모 계정으로 전환하기"),
                    messageBlocks =
                        listOf(
                            MessageBlockUiModel(
                                title = "가족에게",
                                body = "이 계정에는 우리 가족 여행 사진이 많아.\n계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
                            ),
                        ),
                    finalWriteDate = "2025.11.26",
                ),
        )
    }
}
