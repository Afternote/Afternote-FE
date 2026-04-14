package com.afternote.feature.afternote.presentation.author.detail.afternotedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.author.detail.model.AfternoteDetailEvent
import com.afternote.feature.afternote.presentation.author.detail.model.AfternoteDetailUiState
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.navigation.DesignPendingDetailContent
import com.afternote.feature.afternote.presentation.author.navigation.DetailLoadingContent
import com.afternote.feature.afternote.presentation.author.navigation.HandleDeleteResult
import com.afternote.feature.afternote.presentation.shared.detail.AfternoteDetailServiceHeaderWithRecipientChip
import com.afternote.feature.afternote.presentation.shared.detail.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.DetailCard
import com.afternote.feature.afternote.presentation.shared.detail.DetailSectionHeader
import com.afternote.feature.afternote.presentation.shared.detail.EditDropdownMenu
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection
import com.afternote.feature.afternote.presentation.shared.util.getIconResForServiceName

/**
 * 소셜 네트워크 상세 Stateful Route.
 *
 * Now in Android 가이드의 Route + Screen 분리 패턴을 따른다.
 * - 상세/작성자/삭제 상태는 공용 [AfternoteDetailViewModel] 에서 관리한다
 *   (상세 목적지마다 별도의 백스택 엔트리이므로 VM 인스턴스는 화면마다 갈리지만 클래스는 동일).
 *   초기 상세 로드는 ViewModel 의 `init` 과 네비게이션 인자(`itemId`)로만 트리거한다.
 * - UI 는 [SocialNetworkDetailScreen] (Stateless) 에 위임한다.
 * - 방어적으로 `detail.type` 을 검사해 SOCIAL_NETWORK 가 아닌 타입이 들어오면
 *   [DesignPendingDetailContent] 로 폴백한다.
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
                onConsumed = { viewModel.onEvent(AfternoteDetailEvent.DeleteResultConsumed) },
            )

            val detail = state.detail
            if (detail.type != AfternoteServiceType.SOCIAL_NETWORK) {
                DesignPendingDetailContent(onBackClick = onBack)
            } else {
                val method = detail.processing?.method ?: ""

                SocialNetworkDetailScreen(
                    content =
                        SocialNetworkDetailContent(
                            serviceName = detail.title,
                            userName = state.authorDisplayName,
                            accountId = detail.credentials?.id ?: "",
                            password = detail.credentials?.password ?: "",
                            accountProcessingMethod = method,
                            processingMethods = detail.processing?.actions ?: emptyList(),
                            message = detail.processing?.leaveMessage ?: "",
                            finalWriteDate = detail.timestamps.updatedAt.ifEmpty { detail.timestamps.createdAt },
                            afternoteEditReceivers =
                                detail.receivers.map { r ->
                                    AfternoteEditorReceiver(
                                        id = "",
                                        name = r.name,
                                        label = r.relation,
                                    )
                                },
                            iconResId = getIconResForServiceName(detail.title),
                        ),
                    onBackClick = onBack,
                    onEditClick = { onNavigateToEditor(detail.id.toString()) },
                    onDeleteConfirm = { viewModel.onEvent(AfternoteDetailEvent.Delete(detail.id)) },
                )
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
            iconResId = content.iconResId,
            serviceName = content.serviceName,
            finalWriteDate = content.finalWriteDate,
            recipientBadgeState =
                if (content.afternoteEditReceivers.isNotEmpty()) {
                    RecipientDesignationBadgeState.Completed
                } else {
                    RecipientDesignationBadgeState.Incomplete()
                },
        )

        Spacer(modifier = Modifier.height(31.dp))

        AccountSection(
            accountId = content.accountId,
            password = content.password,
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProcessingMethodsSection(methods = content.processingMethods)
        if (content.processingMethods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
        }

        MessageSection(message = content.message)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 소셜 네트워크 상세 전용 ACCOUNT(아이디·비밀번호) 섹션.
 *
 * 비밀번호 표시 토글은 이 블록 안의 로컬 상태로만 둔다 (`shared/detail` 패키지로의 조기 추출은 하지 않음).
 */
@Composable
private fun AccountSection(
    accountId: String,
    password: String,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        DetailSectionHeader(
            iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
            label = stringResource(R.string.feature_afternote_detail_section_account),
        )

        Spacer(modifier = Modifier.height(13.dp))

        DetailCard {
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .size(32.dp)
                            .background(AfternoteDesign.colors.gray2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_user),
                            contentDescription = null,
                            tint = AfternoteDesign.colors.gray9,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.feature_afternote_detail_label_id),
                            style = AfternoteDesign.typography.footnoteCaption,
                            color = AfternoteDesign.colors.gray6,
                        )
                        Text(
                            text = accountId,
                            style = AfternoteDesign.typography.bodySmallB,
                            color = AfternoteDesign.colors.gray9,
                        )
                    }
                }

                HorizontalDivider(
                    color = AfternoteDesign.colors.gray2,
                    thickness = 1.dp,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .size(32.dp)
                            .background(AfternoteDesign.colors.gray2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_ic_deep_thought_moon),
                            contentDescription = null,
                            tint = AfternoteDesign.colors.gray9,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.feature_afternote_detail_label_password),
                            style = AfternoteDesign.typography.footnoteCaption,
                            color = AfternoteDesign.colors.gray6,
                        )
                        Text(
                            text =
                                if (passwordVisible) {
                                    password
                                } else {
                                    stringResource(R.string.feature_afternote_detail_password_mask)
                                },
                            style = AfternoteDesign.typography.bodySmallB,
                            color = AfternoteDesign.colors.gray9,
                        )
                    }
                    Spacer(Modifier.weight(1f))
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
                            Modifier.clickable(onClick = {
                                passwordVisible = !passwordVisible
                            }),
                    )
                }
            }
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
                AfternoteEditorReceiver(
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
