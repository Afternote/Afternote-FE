package com.afternote.feature.afternote.presentation.receiver.detail

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
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.AfternoteDetailServiceHeader
import com.afternote.feature.afternote.presentation.shared.detail.DetailInfoRow
import com.afternote.feature.afternote.presentation.shared.detail.DetailSection
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection
import com.afternote.feature.afternote.presentation.shared.model.AfternoteServiceDisplay

/**
 * 수신 소셜 네트워크 상세 (Stateless).
 *
 * 발신자 [com.afternote.feature.afternote.presentation.detail.account.AccountDetailScreen]
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
                title = stringResource(R.string.afternote_detail_title),
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
            ReceivedAccountSection(credentials = content.credentials)
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
 *
 * [credentials] 가 `null` 이면 — 서버가 수신자에게 자격증명을 내려주지 않았거나 발신자가 적지
 * 않은 경우 — 아이디·비밀번호 행 대신 부재 문구를 그린다. 빈 값에 마스킹을 씌우면 수신자는
 * 가려진 값이 있다고 믿고 "표시" 를 누르지만 아무것도 얻지 못한다 (#619).
 */
@Composable
private fun ReceivedAccountSection(
    credentials: ReceivedAccountCredentialsUiModel?,
    modifier: Modifier = Modifier,
) {
    DetailSection(
        iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
        label = stringResource(R.string.afternote_detail_section_account),
        modifier = modifier,
    ) {
        if (credentials == null) {
            Text(
                text = stringResource(R.string.afternote_receiver_detail_account_absent),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
            )
            return@DetailSection
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailInfoRow(
                iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
                label = stringResource(R.string.afternote_detail_label_id),
                value = credentials.accountId ?: stringResource(R.string.afternote_receiver_detail_account_value_absent),
            )
            HorizontalDivider(
                color = AfternoteDesign.colors.gray2,
                thickness = 1.dp,
            )
            ReceivedPasswordRow(password = credentials.password)
        }
    }
}

/**
 * 비밀번호 행. 값이 있을 때만 마스킹과 표시 토글을 붙인다 — 없는 값을 가리는 시늉을 하지 않는다.
 */
@Composable
private fun ReceivedPasswordRow(
    password: String?,
    modifier: Modifier = Modifier,
) {
    val passwordLabel = stringResource(R.string.afternote_detail_label_password)

    if (password == null) {
        DetailInfoRow(
            iconResId = R.drawable.afternote_ic_lock,
            label = passwordLabel,
            value = stringResource(R.string.afternote_receiver_detail_account_value_absent),
            modifier = modifier,
        )
        return
    }

    var passwordVisible by remember { mutableStateOf(false) }

    DetailInfoRow(
        iconResId = R.drawable.afternote_ic_lock,
        label = passwordLabel,
        value =
            if (passwordVisible) {
                password
            } else {
                stringResource(R.string.afternote_detail_password_mask)
            },
        modifier = modifier,
        trailingContent = {
            val toggleLabel =
                if (passwordVisible) {
                    stringResource(R.string.afternote_detail_password_hide)
                } else {
                    stringResource(R.string.afternote_detail_password_show)
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
