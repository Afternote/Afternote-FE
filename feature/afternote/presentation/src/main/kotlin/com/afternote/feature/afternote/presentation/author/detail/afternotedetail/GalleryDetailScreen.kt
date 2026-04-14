package com.afternote.feature.afternote.presentation.author.detail.afternotedetail

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.badge.RecipientDesignationBadgeState
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.author.detail.model.AfternoteDetailEvent
import com.afternote.feature.afternote.presentation.author.detail.model.AfternoteDetailUiState
import com.afternote.feature.afternote.presentation.author.navigation.DesignPendingDetailContent
import com.afternote.feature.afternote.presentation.author.navigation.DetailLoadingContent
import com.afternote.feature.afternote.presentation.author.navigation.HandleDeleteResult
import com.afternote.feature.afternote.presentation.shared.detail.AfternoteDetailServiceHeaderWithRecipientChip
import com.afternote.feature.afternote.presentation.shared.detail.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.EditDropdownMenu
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection
import com.afternote.feature.afternote.presentation.shared.detail.ReceiversCard
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel
import com.afternote.feature.afternote.presentation.shared.util.getIconResForServiceName

/**
 * 갤러리 상세 Stateful Route.
 *
 * [SocialNetworkDetailRoute] 와 동일하게 공용 [AfternoteDetailViewModel]·[AfternoteDetailUiState] 를 쓰고,
 * 성공 시 [AfternoteServiceType.GALLERY_AND_FILES] 타입을 검사해 폴백한다.
 */
@Composable
internal fun GalleryDetailRoute(
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
            if (detail.type != AfternoteServiceType.GALLERY_AND_FILES) {
                DesignPendingDetailContent(onBackClick = onBack)
            } else {
                GalleryDetailScreen(
                    content =
                        GalleryDetailContent(
                            serviceName = detail.title,
                            userName = state.authorDisplayName,
                            finalWriteDate = detail.timestamps.updatedAt.ifEmpty { detail.timestamps.createdAt },
                            afternoteEditReceivers =
                                detail.receivers.map { r ->
                                    ReceiverUiModel(
                                        id = "",
                                        name = r.name,
                                        label = r.relation,
                                    )
                                },
                            informationProcessingMethod = detail.processing?.method ?: "",
                            processingMethods = detail.processing?.actions ?: emptyList(),
                            message = detail.processing?.leaveMessage ?: "",
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
 * 갤러리 상세 표시 데이터.
 */
@Immutable
data class GalleryDetailContent(
    val serviceName: String = "",
    val userName: String = "",
    val finalWriteDate: String = "",
    val afternoteEditReceivers: List<ReceiverUiModel> = emptyList(),
    val informationProcessingMethod: String = "",
    val processingMethods: List<String> = emptyList(),
    val message: String = "",
)

/**
 * 갤러리 애프터노트 상세 화면 (Stateless).
 *
 * [SocialNetworkDetailScreen] 과 동일한 Scaffold·TopBar·드롭다운·스크롤 modifier 패턴을 따른다.
 */
@Composable
fun GalleryDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: GalleryDetailContent = GalleryDetailContent(),
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
        GalleryDetailScrollContent(
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
private fun GalleryDetailScrollContent(
    content: GalleryDetailContent,
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
            iconResId = getIconResForServiceName(content.serviceName),
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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ReceiversCard(receivers = content.afternoteEditReceivers)
            ProcessingMethodsSection(methods = content.processingMethods)
            MessageSection(message = content.message)
        }
    }
}

// endregion

internal val GALLERY_PREVIEW_CONTENT =
    GalleryDetailContent(
        serviceName = "갤러리",
        userName = "서영",
        finalWriteDate = "2025.11.26",
        informationProcessingMethod = "TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER",
        processingMethods = listOf("'엽사' 폴더 박선호에게 전송", "'흑역사' 폴더 삭제"),
        afternoteEditReceivers =
            listOf(
                ReceiverUiModel(id = "1", name = "김지은", label = "친구"),
                ReceiverUiModel(id = "2", name = "김혜성", label = "친구"),
            ),
    )

@Preview(showBackground = true)
@Composable
private fun GalleryDetailScreenPreview() {
    AfternoteTheme {
        GalleryDetailScreen(
            content = GALLERY_PREVIEW_CONTENT,
            onBackClick = {},
            onEditClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Edit Dropdown Menu")
@Composable
private fun GalleryDetailScreenWithDropdownPreview() {
    AfternoteTheme {
        val stateWithDropdown =
            remember {
                AfternoteDetailState().apply {
                    toggleDropdownMenu()
                }
            }
        GalleryDetailScreen(
            content = GALLERY_PREVIEW_CONTENT,
            onBackClick = {},
            onEditClick = {},
            state = stateWithDropdown,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryDetailScreenWithDeleteDialogPreview() {
    AfternoteTheme {
        val stateWithDialog =
            remember {
                AfternoteDetailState().apply {
                    showDeleteDialog()
                }
            }
        GalleryDetailScreen(
            content = GALLERY_PREVIEW_CONTENT,
            onBackClick = {},
            onEditClick = {},
            state = stateWithDialog,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryDetailScreenReceiverModePreview() {
    AfternoteTheme {
        GalleryDetailScreen(
            content = GALLERY_PREVIEW_CONTENT,
            isEditable = false,
            onBackClick = {},
            onEditClick = {},
            state = rememberAfternoteDetailState(),
        )
    }
}
