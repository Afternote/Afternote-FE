package com.afternote.feature.afternote.presentation.detail

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.popup.AfternoteActionMenu
import com.afternote.core.ui.popup.editDeleteActionMenuItems
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.AfternoteDetailServiceHeader
import com.afternote.feature.afternote.presentation.shared.detail.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection
import com.afternote.feature.afternote.presentation.shared.detail.ReceiversCard
import com.afternote.feature.afternote.presentation.shared.model.AfternoteServiceDisplay
import com.afternote.feature.afternote.presentation.shared.model.MessageBlockUiModel
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

/**
 * 갤러리 상세 표시 데이터.
 */
@Immutable
data class GalleryDetailContent(
    val serviceName: String = "",
    val finalWriteDate: String = "",
    val afternoteEditReceivers: List<ReceiverUiModel> = emptyList(),
    val processingMethods: List<String> = emptyList(),
    val messageBlocks: List<MessageBlockUiModel> = emptyList(),
)

/**
 * 갤러리 애프터노트 상세 화면 (Stateless).
 *
 * [com.afternote.feature.afternote.presentation.detail.account.AccountDetailScreen] 과 동일한 Scaffold·TopBar·드롭다운·스크롤 modifier 패턴을 따른다.
 */
@Composable
fun GalleryDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: GalleryDetailContent = GalleryDetailContent(),
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
        AfternoteDetailServiceHeader(
            service =
                AfternoteServiceDisplay.fromService(
                    serviceName = content.serviceName,
                    type = AfternoteType.GALLERY_AND_FILES,
                ),
            finalWriteDate = content.finalWriteDate,
        )

        Spacer(modifier = Modifier.height(31.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ReceiversCard(receivers = content.afternoteEditReceivers)
            ProcessingMethodsSection(methods = content.processingMethods)
            MessageSection(blocks = content.messageBlocks)
        }
    }
}

// endregion
