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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.AfternoteEmbeddedMainBottomBar
import com.afternote.feature.afternote.presentation.shared.detail.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.DetailCard
import com.afternote.feature.afternote.presentation.shared.detail.DetailSectionHeader
import com.afternote.feature.afternote.presentation.shared.detail.EditDropdownMenu

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
                                    Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.feature_afternote_detail_edit),
                                    modifier = Modifier.size(20.dp),
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
        bottomBar = {
            AfternoteEmbeddedMainBottomBar(
                selectedNavTab = state.selectedNavItem,
                onTabClick = state::onNavItemSelected,
            )
        },
    ) { paddingValues ->
        SocialNetworkDetailScrollContent(
            content = content,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

// region — Scroll content

@Composable
private fun SocialNetworkDetailScrollContent(
    content: SocialNetworkDetailContent,
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
        Spacer(modifier = Modifier.height(16.dp))

        // — 서비스 아이콘 + 이름 + 날짜
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(content.iconResId),
                contentDescription = content.serviceName,
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = content.serviceName,
                    style = AfternoteDesign.typography.h3,
                    color = AfternoteDesign.colors.gray9,
                )
                Text(
                    text =
                        stringResource(
                            R.string.afternote_last_written_date,
                            content.finalWriteDate,
                        ),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray5,
                )
            }
        }

        // — 수신인 지정 완료 뱃지
        if (content.afternoteEditReceivers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier =
                    Modifier
                        .border(
                            width = 1.dp,
                            color = AfternoteDesign.colors.gray3,
                            shape = RoundedCornerShape(20.dp),
                        ).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_check_circle),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AfternoteDesign.colors.gray7,
                )
                Text(
                    text = stringResource(R.string.feature_afternote_detail_receiver_assigned),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray7,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // — ACCOUNT 섹션
        DetailSectionHeader(
            iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
            label = stringResource(R.string.feature_afternote_detail_section_account),
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailCard {
            Column {
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
                HorizontalDivider(color = AfternoteDesign.colors.gray2)
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
        if (content.processingMethods.isNotEmpty()) {
            DetailSectionHeader(
                iconResId = com.afternote.core.ui.R.drawable.core_ui_settings,
                label = stringResource(R.string.feature_afternote_detail_section_processing),
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content.processingMethods.forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_check_circle),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = AfternoteDesign.colors.gray9,
                            )
                            Text(
                                text = method,
                                style = AfternoteDesign.typography.bodySmallR,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // — 남기신 말씀 섹션
        DetailSectionHeader(
            iconResId = com.afternote.core.ui.R.drawable.core_ui_ic_mail,
            label = stringResource(R.string.feature_afternote_detail_section_message),
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                val hasMessage = content.message.isNotEmpty()
                val displayMessage =
                    if (hasMessage) content.message else stringResource(R.string.feature_afternote_detail_no_message)
                val textColor =
                    if (hasMessage) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray5
                Row(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.feature_afternote_detail_quote_mark),
                        style = AfternoteDesign.typography.bodyLargeR,
                        color = AfternoteDesign.colors.gray4,
                    )
                    Text(
                        text = displayMessage,
                        style = AfternoteDesign.typography.bodySmallR,
                        color = textColor,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(R.drawable.feature_afternote_img_logo),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// endregion

@Preview
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

@Preview(name = "Edit Dropdown Menu")
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

@Preview
@Composable
private fun SocialNetworkDetailScreenWithDeleteDialogPreview() {
    AfternoteTheme {
        val stateWithDialog =
            remember {
                AfternoteDetailState().apply {
                    showDeleteDialog()
                }
            }
        SocialNetworkDetailScreen(
            content = PREVIEW_CONTENT,
            onBackClick = {},
            onEditClick = {},
            state = stateWithDialog,
        )
    }
}

@Preview
@Composable
private fun SocialNetworkDetailScreenReceiverModePreview() {
    AfternoteTheme {
        SocialNetworkDetailScreen(
            content = PREVIEW_CONTENT,
            isEditable = false,
            onBackClick = {},
            state =
                rememberAfternoteDetailState(
                    defaultBottomNavItem = BottomNavTab.NOTE,
                ),
        )
    }
}
