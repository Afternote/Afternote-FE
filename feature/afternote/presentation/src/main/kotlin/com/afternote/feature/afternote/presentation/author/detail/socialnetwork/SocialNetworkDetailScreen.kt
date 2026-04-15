package com.afternote.feature.afternote.presentation.author.detail.socialnetwork

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.badge.RecipientDesignationBadgeState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailState
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailUiState
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.author.detail.DetailContentUiModel
import com.afternote.feature.afternote.presentation.author.detail.rememberAfternoteDetailState
import com.afternote.feature.afternote.presentation.author.navigation.DesignPendingDetailContent
import com.afternote.feature.afternote.presentation.author.navigation.DetailLoadingContent
import com.afternote.feature.afternote.presentation.author.navigation.HandleDeleteResult
import com.afternote.feature.afternote.presentation.shared.detail.AfternoteDetailServiceHeaderWithRecipientChip
import com.afternote.feature.afternote.presentation.shared.detail.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.DetailInfoRow
import com.afternote.feature.afternote.presentation.shared.detail.DetailSection
import com.afternote.feature.afternote.presentation.shared.detail.EditDropdownMenu
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection
import com.afternote.feature.afternote.presentation.shared.model.AfternoteServiceDisplay
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

/**
 * 소셜 네트워크 상세 Stateful Route.
 *
 * Now in Android 가이드의 Route + Screen 분리 패턴을 따른다.
 * - 상세/작성자/삭제 상태는 공용 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel] 에서 관리한다
 *   (상세 목적지마다 별도의 백스택 엔트리이므로 VM 인스턴스는 화면마다 갈리지만 클래스는 동일).
 *   초기 상세 로드는 ViewModel 의 `init` 과 네비게이션 인자(`itemId`)로만 트리거한다.
 * - UI 는 [SocialNetworkDetailScreen] (Stateless) 에 위임한다.
 * - [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailUiState.Success.contentUiModel] 이 소셜이 아니면 [DesignPendingDetailContent] 로 폴백한다.
 */
@Composable
internal fun SocialNetworkDetailRoute(
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
    viewModel: AfternoteDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        AfternoteDetailUiState.Loading -> {
            DetailLoadingContent()
        }

        is AfternoteDetailUiState.Error -> {
            DesignPendingDetailContent(onBackClick = onBack)
        }

        is AfternoteDetailUiState.Success -> {
            HandleDeleteResult(
                deleteState = state.deleteState,
                onBack = onBack,
                onConsumed = viewModel::consumeDeleteResult,
            )

            when (val model = state.contentUiModel) {
                is DetailContentUiModel.SocialNetwork -> {
                    SocialNetworkDetailScreen(
                        content = model.content,
                        onBackClick = onBack,
                        onEditClick = { onNavigateToEditor(state.detailId.toString()) },
                        onDeleteConfirm = { viewModel.deleteAfternote(state.detailId) },
                    )
                }

                else -> {
                    DesignPendingDetailContent(onBackClick = onBack)
                }
            }
        }
    }
}

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
        SocialNetworkDetailScrollContent(
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
private fun SocialNetworkDetailScrollContent(
    content: SocialNetworkDetailContent,
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
        AfternoteDetailServiceHeaderWithRecipientChip(
            service =
                AfternoteServiceDisplay(
                    serviceName = content.serviceName,
                    iconResId = content.iconResId,
                ),
            finalWriteDate = content.finalWriteDate,
            recipientBadgeState =
                if (content.afternoteEditReceivers.isNotEmpty()) {
                    RecipientDesignationBadgeState.Completed
                } else {
                    RecipientDesignationBadgeState.Incomplete()
                },
        )

        Spacer(modifier = Modifier.height(31.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountSection(
                accountId = content.accountId,
                password = content.password,
            )
            ProcessingMethodsSection(methods = content.processingMethods)
            MessageSection(message = content.message)
        }
    }
}

/**
 * 소셜 네트워크 상세 전용 ACCOUNT(아이디·비밀번호) 섹션.
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
                iconResId = com.afternote.core.ui.R.drawable.core_ui_ic_deep_thought_moon,
                label = stringResource(R.string.feature_afternote_detail_label_password),
                value =
                    if (passwordVisible) {
                        password
                    } else {
                        stringResource(R.string.feature_afternote_detail_password_mask)
                    },
                trailingContent = {
                    Text(
                        text =
                            if (passwordVisible) {
                                stringResource(R.string.feature_afternote_detail_password_hide)
                            } else {
                                stringResource(R.string.feature_afternote_detail_password_show)
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

/**
 * Android Studio @Preview 전용. Debug 빌드 런타임 목업은
 * `AfternoteDebugMockNetworkInterceptor`의 GET `/api/afternotes/{id}` (예: id 1, 3)를 사용한다.
 */
private val PreviewSocialNetworkInstaContent =
    SocialNetworkDetailContent(
        serviceName = "인스타그램",
        userName = "서영",
        accountId = "qwerty123",
        password = "qwerty123",
        accountProcessingMethod = "MEMORIAL",
        processingMethods = listOf("게시물 내리기", "추모 게시물 올리기", "추모 계정으로 전환하기"),
        message = "이 계정에는 우리 가족 여행 사진이 많아.\n계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
        finalWriteDate = "2025.11.26",
        afternoteEditReceivers =
            listOf(
                ReceiverUiModel(
                    id = "1",
                    name = "황규운",
                    label = "친구",
                ),
            ),
    )

@Preview(showBackground = true)
@Composable
private fun SocialNetworkDetailScreenPreview() {
    AfternoteTheme {
        SocialNetworkDetailScreen(
            content = PreviewSocialNetworkInstaContent,
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
            content = PreviewSocialNetworkInstaContent,
            onBackClick = {},
            onEditClick = {},
            state = stateWithDropdown,
        )
    }
}
