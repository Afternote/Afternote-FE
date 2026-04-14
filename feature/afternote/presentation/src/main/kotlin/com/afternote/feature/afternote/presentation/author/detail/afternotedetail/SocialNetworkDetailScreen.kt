package com.afternote.feature.afternote.presentation.author.detail.afternotedetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.badge.RecipientDesignationBadge
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.DetailCard
import com.afternote.feature.afternote.presentation.shared.detail.DetailSectionHeader
import com.afternote.feature.afternote.presentation.shared.detail.EditDropdownMenu
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection

/**
 * 소셜 네트워크 애프터노트 상세 화면.
 */
@Composable
fun SocialNetworkDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: SocialNetworkDetailContent = SocialNetworkDetailContent(),
    isEditable: Boolean = true,
    onEditClick: () -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
    state: AfternoteDetailState = rememberAfternoteDetailState(),
) {
    if (isEditable && state.showDeleteDialog) {
        DeleteConfirmDialog(
            serviceName = content.serviceName,
            onDismiss = state::hideDeleteDialog,
            onConfirm = {
                state.hideDeleteDialog()
                onDeleteConfirm()
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.feature_afternote_detail_title),
                onBackClick = onBackClick,
                actions = {
                    if (isEditable) {
                        Box {
                            IconButton(onClick = state::toggleDropdownMenu) {
                                Icon(
                                    painter = painterResource(R.drawable.afternote_ui_detail_edit),
                                    contentDescription = stringResource(R.string.feature_afternote_detail_edit),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            EditDropdownMenu(
                                expanded = state.showDropdownMenu,
                                onDismissRequest = state::hideDropdownMenu,
                                onDeleteClick = { state.showDeleteDialog() },
                                onEditClick = onEditClick,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        // Edge-to-Edge: 스크롤 컨테이너 자체를 paddingValues 로 잘라내면 탑바 아래 영역이 스크롤 밖으로 밀려
        // 오버스크롤·제스처 네비게이션 UX 가 어색해진다. 스크롤 내부 Spacer 로 인셋을 소비해 콘텐츠가
        // 탑바 뒤로 자연스럽게 지나가도록 한다.
        SocialNetworkDetailScrollContent(
            content = content,
            topInset = paddingValues.calculateTopPadding(),
            bottomInset = paddingValues.calculateBottomPadding(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// region — Scroll content

@Composable
private fun SocialNetworkDetailScrollContent(
    content: SocialNetworkDetailContent,
    topInset: Dp,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(topInset + 24.dp))

        // — 서비스 아이콘 + 이름 + 날짜
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .size(64.dp)
                    .border(1.dp, shape = CircleShape, color = AfternoteDesign.colors.gray2),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(content.iconResId),
                    contentDescription = content.serviceName,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = content.serviceName,
                    style = AfternoteDesign.typography.h2,
                    color = AfternoteDesign.colors.gray9,
                )
                Text(
                    text =
                        stringResource(
                            R.string.afternote_last_written_date,
                            content.finalWriteDate,
                        ),
                    style = AfternoteDesign.typography.inter,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        }

        // — 수신인 지정 완료 뱃지
        if (content.afternoteEditReceivers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            RecipientDesignationBadge(
                text = stringResource(content.badgeTextResId),
            )
        }

        Spacer(modifier = Modifier.height(31.dp))

        // — ACCOUNT 섹션
        DetailSectionHeader(
            iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
            label = stringResource(R.string.feature_afternote_detail_section_account),
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailCard {
            Column {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .bottomBorder(color = AfternoteDesign.colors.gray2, width = 1.dp),
                ) {
                    // 아이디
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_user),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = AfternoteDesign.colors.gray5,
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.feature_afternote_detail_label_id),
                                style = AfternoteDesign.typography.footnoteCaption,
                                color = AfternoteDesign.colors.gray5,
                            )
                            Text(
                                text = content.accountId,
                                style = AfternoteDesign.typography.bodySmallB,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 비밀번호
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.feature_afternote_ic_social_pattern),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = AfternoteDesign.colors.gray5,
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.feature_afternote_detail_label_password),
                                style = AfternoteDesign.typography.footnoteCaption,
                                color = AfternoteDesign.colors.gray5,
                            )
                            Text(
                                text =
                                    if (passwordVisible) {
                                        content.password
                                    } else {
                                        stringResource(
                                            R.string.feature_afternote_detail_password_mask,
                                        )
                                    },
                                style = AfternoteDesign.typography.bodySmallB,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }
                    }
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text =
                                if (passwordVisible) {
                                    stringResource(R.string.feature_afternote_detail_password_hide)
                                } else {
                                    stringResource(R.string.feature_afternote_detail_password_show)
                                },
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.b1,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // — 처리방법 섹션
        ProcessingMethodsSection(methods = content.processingMethods)

        // — 남기신 말씀 섹션
        MessageSection(message = content.message)

        // 하단 인셋(제스처 네비게이션 바 등) 을 스크롤 내부에서 소비.
        Spacer(modifier = Modifier.height(bottomInset))
    }
}

// endregion

@Preview(showBackground = true)
@Composable
private fun SocialNetworkDetailScreenPreview() {
    AfternoteTheme {
        SocialNetworkDetailScreen(
            content = PREVIEW_CONTENT,
            onBackClick = {},
            onEditClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SocialNetworkDetailScreenWithDropdownPreview() {
    AfternoteTheme {
        val stateWithDropdown =
            remember {
                AfternoteDetailState().apply {
                    toggleDropdownMenu()
                }
            }
        SocialNetworkDetailScreen(
            content = PREVIEW_CONTENT,
            onBackClick = {},
            onEditClick = {},
            state = stateWithDropdown,
        )
    }
}
