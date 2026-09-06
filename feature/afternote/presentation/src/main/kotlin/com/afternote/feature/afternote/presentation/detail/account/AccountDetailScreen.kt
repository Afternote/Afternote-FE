package com.afternote.feature.afternote.presentation.detail.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.popup.AfternoteActionMenu
import com.afternote.core.ui.popup.editDeleteActionMenuItems
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.detail.AfternoteDetailState
import com.afternote.feature.afternote.presentation.detail.rememberAfternoteDetailState
import com.afternote.feature.afternote.presentation.shared.detail.AfternoteDetailServiceHeader
import com.afternote.feature.afternote.presentation.shared.detail.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.DetailInfoRow
import com.afternote.feature.afternote.presentation.shared.detail.DetailSection
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection
import com.afternote.feature.afternote.presentation.shared.model.AfternoteServiceDisplay

/**
 * 소셜 네트워크·비즈니스 애프터노트 공용 상세 화면.
 */
@Composable
fun AccountDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: AccountDetailContent = AccountDetailContent(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    isEditable: Boolean = true,
    onEditClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.afternote_detail_title),
                onBackClick = onBackClick,
                actions = {
                    if (isEditable) {
                        Box {
                            IconButton(onClick = state::toggleDropdownMenu) {
                                Icon(
                                    painter = painterResource(R.drawable.afternote_ic_detail_edit),
                                    contentDescription = stringResource(R.string.afternote_detail_edit),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            AfternoteActionMenu(
                                expanded = state.showDropdownMenu,
                                onDismissRequest = state::hideDropdownMenu,
                                items =
                                    editDeleteActionMenuItems(
                                        onEditClick = onEditClick,
                                        onDeleteClick = { state.showDeleteDialog() },
                                    ),
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        AccountDetailScrollContent(
            content = content,
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        )
    }
}

// region — Scroll content

@Composable
private fun AccountDetailScrollContent(
    content: AccountDetailContent,
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
            service = AfternoteServiceDisplay.fromService(content.serviceName, content.type),
            finalWriteDate = content.finalWriteDate,
        )

        Spacer(modifier = Modifier.height(31.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountSection(
                accountId = content.accountId,
                password = content.password,
            )
            ProcessingMethodsSection(methods = content.processingMethods)
            MessageSection(blocks = content.messageBlocks)
        }
    }
}

/**
 * 소셜 네트워크·비즈니스 상세 공용 ACCOUNT(아이디·비밀번호) 섹션.
 *
 * 섹션 뼈대는 공용 [DetailSection], 행 레이아웃은 [DetailInfoRow] 를 쓰고,
 * 비밀번호 표시 토글 상태·동작만 이 블록에 둔다.
 */
@Composable
private fun AccountSection(
    accountId: String,
    password: String,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    DetailSection(
        iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
        label = stringResource(R.string.afternote_detail_section_account),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailInfoRow(
                iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
                label = stringResource(R.string.afternote_detail_label_id),
                value = accountId,
            )
            HorizontalDivider(
                color = AfternoteDesign.colors.gray2,
                thickness = 1.dp,
            )
            DetailInfoRow(
                iconResId = R.drawable.afternote_ic_lock,
                label = stringResource(R.string.afternote_detail_label_password),
                value =
                    if (passwordVisible) {
                        password
                    } else {
                        stringResource(R.string.afternote_detail_password_mask)
                    },
                trailingContent = {
                    Text(
                        text =
                            if (passwordVisible) {
                                stringResource(R.string.afternote_detail_password_hide)
                            } else {
                                stringResource(R.string.afternote_detail_password_show)
                            },
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.b1,
                        modifier =
                            Modifier.clickable {
                                passwordVisible = !passwordVisible
                            },
                    )
                },
            )
        }
    }
}

// endregion
