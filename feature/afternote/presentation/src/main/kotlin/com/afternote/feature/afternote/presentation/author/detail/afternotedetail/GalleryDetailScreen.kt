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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteLightTheme
import com.afternote.feature.afternote.presentation.shared.detail.DetailSection
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection
import com.afternote.feature.afternote.presentation.shared.detail.components.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.components.EditDropdownMenu
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

/**
 * 갤러리 상세 화면의 데이터 상태
 */
@Immutable
data class GalleryDetailState(
    val serviceName: String = "",
    val userName: String = "",
    val finalWriteDate: String = "",
    val afternoteEditReceivers: List<ReceiverUiModel> = emptyList(),
    val informationProcessingMethod: String = "",
    val processingMethods: List<String> = emptyList(),
    val message: String = "",
)

/**
 * 갤러리 상세 화면의 콜백 모음
 */
@Immutable
data class GalleryDetailCallbacks(
    val onBackClick: () -> Unit,
    val onEditClick: () -> Unit,
    val onDeleteConfirm: () -> Unit = {},
)

/**
 * 갤러리 상세 화면
 */
@Composable
fun GalleryDetailScreen(
    detailState: GalleryDetailState,
    callbacks: GalleryDetailCallbacks,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
    uiState: AfternoteDetailState = rememberAfternoteDetailState(),
) {
    if (isEditable && uiState.showDeleteDialog) {
        DeleteConfirmDialog(
            serviceName = detailState.serviceName,
            onDismiss = uiState::hideDeleteDialog,
            onConfirm = {
                uiState.hideDeleteDialog()
                callbacks.onDeleteConfirm()
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.feature_afternote_detail_title),
                onBackClick = callbacks.onBackClick,
                actions = {
                    if (isEditable) {
                        Box {
                            IconButton(onClick = uiState::toggleDropdownMenu) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.feature_afternote_detail_edit),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            EditDropdownMenu(
                                expanded = uiState.showDropdownMenu,
                                onDismissRequest = uiState::hideDropdownMenu,
                                onDeleteClick = { uiState.showDeleteDialog() },
                                onEditClick = { callbacks.onEditClick() },
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        GalleryDetailScrollContent(
            detailState = detailState,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

// region — Scroll content

@Composable
private fun GalleryDetailScrollContent(
    detailState: GalleryDetailState,
    modifier: Modifier = Modifier,
) {
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
                painter = painterResource(R.drawable.feature_afternote_ic_gallery_pattern),
                contentDescription = detailState.serviceName,
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = detailState.serviceName,
                    style = AfternoteDesign.typography.h3,
                    color = AfternoteDesign.colors.gray9,
                )
                Text(
                    text =
                        stringResource(
                            R.string.afternote_last_written_date,
                            detailState.finalWriteDate,
                        ),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray5,
                )
            }
        }

        // — 추가 수신자에게 정보 전달 뱃지
        if (detailState.afternoteEditReceivers.isNotEmpty()) {
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
                AfternoteCircularCheckbox(
                    state = CheckboxState.Default,
                    size = 20.dp,
                    onClick = null,
                )
                Text(
                    text = stringResource(R.string.feature_afternote_detail_additional_receiver_badge),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray7,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // — 추가 수신자 섹션
        if (detailState.afternoteEditReceivers.isNotEmpty()) {
            DetailSection(
                iconResId = com.afternote.core.ui.R.drawable.core_ui_user,
                label = stringResource(R.string.feature_afternote_detail_section_additional_receiver),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    detailState.afternoteEditReceivers.forEach { receiver ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(24.dp)),
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.feature_afternote_img_recipient_profile),
                                    contentDescription = stringResource(R.string.feature_afternote_content_description_recipient_profile),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Column {
                                Text(
                                    text = receiver.name,
                                    style = AfternoteDesign.typography.bodySmallB,
                                    color = AfternoteDesign.colors.gray9,
                                )
                                Text(
                                    text = receiver.label,
                                    style = AfternoteDesign.typography.captionLargeR,
                                    color = AfternoteDesign.colors.gray5,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        ProcessingMethodsSection(methods = detailState.processingMethods)
        if (detailState.processingMethods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
        }

        MessageSection(message = detailState.message)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// endregion

internal val GALLERY_PREVIEW_CONTENT =
    GalleryDetailState(
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

@Preview
@Composable
private fun GalleryDetailScreenPreview() {
    AfternoteLightTheme {
        GalleryDetailScreen(
            detailState = GALLERY_PREVIEW_CONTENT,
            callbacks =
                GalleryDetailCallbacks(
                    onBackClick = {},
                    onEditClick = {},
                ),
        )
    }
}

@Preview(name = "Edit Dropdown Menu")
@Composable
private fun GalleryDetailScreenWithDropdownPreview() {
    AfternoteLightTheme {
        val uiState =
            remember {
                AfternoteDetailState().apply {
                    toggleDropdownMenu()
                }
            }
        GalleryDetailScreen(
            detailState = GALLERY_PREVIEW_CONTENT,
            callbacks =
                GalleryDetailCallbacks(
                    onBackClick = {},
                    onEditClick = {},
                ),
            uiState = uiState,
        )
    }
}

@Preview
@Composable
private fun GalleryDetailScreenWithDeleteDialogPreview() {
    AfternoteLightTheme {
        val uiState =
            remember {
                AfternoteDetailState().apply {
                    showDeleteDialog()
                }
            }
        GalleryDetailScreen(
            detailState = GALLERY_PREVIEW_CONTENT,
            callbacks =
                GalleryDetailCallbacks(
                    onBackClick = {},
                    onEditClick = {},
                ),
            uiState = uiState,
        )
    }
}

@Preview
@Composable
private fun GalleryDetailScreenReceiverModePreview() {
    AfternoteLightTheme {
        GalleryDetailScreen(
            detailState = GALLERY_PREVIEW_CONTENT,
            isEditable = false,
            callbacks =
                GalleryDetailCallbacks(
                    onBackClick = {},
                    onEditClick = {},
                ),
            uiState =
                rememberAfternoteDetailState(
                    defaultBottomNavItem = BottomNavTab.NOTE,
                ),
        )
    }
}
