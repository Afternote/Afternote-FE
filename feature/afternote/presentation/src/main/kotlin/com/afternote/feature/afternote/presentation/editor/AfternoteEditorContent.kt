package com.afternote.feature.afternote.presentation.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.modifierextention.shimmerLoadingPlaceholder
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.account.AccountEditorContent
import com.afternote.feature.afternote.presentation.editor.account.AccountSection
import com.afternote.feature.afternote.presentation.editor.gallery.GalleryAndFileEditorContent
import com.afternote.feature.afternote.presentation.editor.mapper.hasServiceSelection
import com.afternote.feature.afternote.presentation.editor.memorial.MemorialEditorContent
import com.afternote.feature.afternote.presentation.editor.memorial.MemorialMediaSourceSheet
import com.afternote.feature.afternote.presentation.editor.memorial.MemorialMediaTarget
import com.afternote.feature.afternote.presentation.editor.memorial.rememberMemorialMediaSourceState
import com.afternote.feature.afternote.presentation.editor.memorial.removableMemorialMediaTargets
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.editor.selection.DropdownMenuStyle
import com.afternote.feature.afternote.presentation.editor.selection.EditorSelectionDropdown
import com.afternote.feature.afternote.presentation.editor.selection.EditorServiceSelectionField
import com.afternote.feature.afternote.presentation.editor.selection.EditorServiceSelectionSheet
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState

@Composable
internal fun EditorContent(
    state: AfternoteEditorState,
    form: EditorFormState,
    typeContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isPrefillLoading: Boolean = false,
    isTypeSelectionEnabled: Boolean = true,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        EditorSelectionDropdown(
            label = stringResource(R.string.afternote_editor_label_category),
            selectedValue = form.selectedType,
            options = AfternoteType.entries,
            optionLabel = { it.toDropdownLabel() },
            onValueSelected = state::onTypeSelected,
            expanded = state.typeDropdownExpanded,
            onExpandedChange = state::onTypeDropdownExpandedChange,
            enabled = isTypeSelectionEnabled,
            menuStyle =
                DropdownMenuStyle(
                    shadowElevation = 10.dp,
                    tonalElevation = 10.dp,
                ),
        )

        if (isPrefillLoading) {
            EditorPrefillSkeleton(type = form.selectedType)
            return@Column
        }

        if (form.selectedType.hasServiceSelection) {
            Spacer(modifier = Modifier.height(20.dp))

            EditorServiceSelectionField(
                selectedService = form.selectedService,
                onClick = state::openServiceSelectionSheet,
                placeholder =
                    stringResource(
                        R.string.afternote_editor_service_placeholder,
                        form.selectedType.toDropdownLabel(),
                    ),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        typeContent()
    }
}

@Composable
fun AfternoteEditorBody(
    state: AfternoteEditorState,
    form: EditorFormState,
    onNavigateToMemorialPlaylist: () -> Unit,
    onNavigateToSelectReceiver: () -> Unit,
    onThumbnailBytesReady: (ByteArray?) -> Unit,
    onThumbnailExtractionFailed: (Throwable) -> Unit,
    thumbnailRetryToken: Int,
    onCaptureFailed: (Throwable) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    isPrefillLoading: Boolean = false,
    isTypeSelectionEnabled: Boolean = true,
) {
    // 슬롯을 누르면 곧장 갤러리가 뜨는 대신 "갤러리에서 선택 / 촬영" 시트를 한 단계 끼운다 (#369).
    // 지울 수 있는 첨부가 있으면 같은 시트에 "삭제" 갈래가 더해진다 (#1114).
    val mediaSourceState =
        rememberMemorialMediaSourceState(
            snackbarHostState = snackbarHostState,
            onPhotoSelected = state.setMemorialPhoto,
            onVideoSelected = state.setMemorialVideo,
            onAudioSelected = state.setMemorialAudio,
            onPhotoRemoved = state.removeMemorialPhoto,
            onVideoRemoved = state.removeMemorialVideo,
            onAudioRemoved = state.removeMemorialAudio,
            onCaptureFailed = onCaptureFailed,
        )

    EditorContent(
        state = state,
        form = form,
        typeContent = {
            AfternoteTypeContent(
                state = state,
                form = form,
                onNavigateToMemorialPlaylist = onNavigateToMemorialPlaylist,
                onNavigateToSelectReceiver = onNavigateToSelectReceiver,
                onPhotoAddClick = { mediaSourceState.open(MemorialMediaTarget.PHOTO) },
                onVideoAddClick = { mediaSourceState.open(MemorialMediaTarget.VIDEO) },
                onAudioAddClick = { mediaSourceState.open(MemorialMediaTarget.AUDIO) },
                onThumbnailBytesReady = onThumbnailBytesReady,
                onThumbnailExtractionFailed = onThumbnailExtractionFailed,
                thumbnailRetryToken = thumbnailRetryToken,
            )
        },
        modifier = modifier,
        isPrefillLoading = isPrefillLoading,
        isTypeSelectionEnabled = isTypeSelectionEnabled,
    )

    MemorialMediaSourceSheet(
        state = mediaSourceState,
        removableTargets = form.removableMemorialMediaTargets(),
    )
    EditorServiceSelectionSheet(
        visible = state.isServiceSelectionSheetVisible,
        type = form.selectedType,
        services = form.currentServiceOptions,
        searchQueryState = state.serviceSearchQueryState,
        onDismissRequest = state::dismissServiceSelectionSheet,
        onServiceSelected = state::onServiceSelected,
    )
}

/**
 * 수정 모드 진입 시 `getDetail()` 응답이 도착하기 전까지 prefill 대상 섹션을 가리는 skeleton.
 * 카테고리 드롭다운은 navArg 로 즉시 채워지므로 본 컴포저블 위쪽에서 그대로 노출하고,
 * 본 컴포저블은 그 아래(서비스명·계정·처리 방법·메시지 등)를 카테고리별 대략적인 layout 으로 placeholder 처리한다.
 * [shimmerLoadingPlaceholder] 로 가벼운 shimmer 애니메이션을 적용.
 */
@Composable
private fun EditorPrefillSkeleton(
    type: AfternoteType,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(20.dp))

        if (type.hasServiceSelection) {
            // 서비스명 드롭다운 자리.
            SkeletonBar(height = 56.dp)
            Spacer(modifier = Modifier.height(32.dp))
        }

        when (type) {
            AfternoteType.MEMORIAL -> MemorialPrefillSkeleton()

            AfternoteType.GALLERY_AND_FILES -> GalleryPrefillSkeleton()

            // BUSINESS 는 SOCIAL 과 같은 구조(계정 2필드 + 수신자 지정 + 처리 방법 + 메시지)라 skeleton 도 공유한다.
            AfternoteType.SOCIAL_NETWORK, AfternoteType.BUSINESS -> AccountPrefillSkeleton()

            // ESTATE 는 로드가 끝나도 채울 폼이 없는 "준비 중" placeholder 라(UnimplementedTypeContent)
            // 로딩 동안 흉내 낼 뼈대도 없다 — 아무것도 그리지 않는다. 생성이 차단돼 수정 진입으로
            // 실제 도달할 일은 사실상 없지만, exhaustive when 이라 분기를 명시한다.
            AfternoteType.ESTATE -> Unit
        }
    }
}

@Composable
private fun AccountPrefillSkeleton() {
    // 계정 ID/PW.
    SkeletonBar(height = 56.dp)
    Spacer(modifier = Modifier.height(12.dp))
    SkeletonBar(height = 56.dp)
    Spacer(modifier = Modifier.height(28.dp))
    // 수신자 지정.
    SkeletonBar(height = 72.dp)
    Spacer(modifier = Modifier.height(28.dp))
    // 처리 방법 리스트.
    SkeletonProcessingMethodList()
    Spacer(modifier = Modifier.height(28.dp))
    // 메시지.
    SkeletonBar(height = 140.dp)
}

@Composable
private fun GalleryPrefillSkeleton() {
    // 수신자 지정.
    SkeletonBar(height = 72.dp)
    Spacer(modifier = Modifier.height(28.dp))
    // 처리 방법 리스트.
    SkeletonProcessingMethodList()
    Spacer(modifier = Modifier.height(28.dp))
    // 메시지.
    SkeletonBar(height = 140.dp)
}

@Composable
private fun MemorialPrefillSkeleton() {
    // 영정사진.
    SkeletonBar(height = 180.dp)
    Spacer(modifier = Modifier.height(20.dp))
    // 장례식에 남길 영상.
    SkeletonBar(height = 120.dp)
    Spacer(modifier = Modifier.height(20.dp))
    // 추억 플레이리스트.
    SkeletonBar(height = 96.dp)
    Spacer(modifier = Modifier.height(20.dp))
    // 남기실 말씀.
    SkeletonBar(height = 140.dp)
    Spacer(modifier = Modifier.height(20.dp))
    // 마지막 인사 (라디오 + custom text).
    SkeletonBar(height = 140.dp)
}

@Composable
private fun SkeletonProcessingMethodList() {
    repeat(3) { index ->
        SkeletonBar(height = 48.dp)
        if (index != 2) Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SkeletonBar(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .shimmerLoadingPlaceholder(),
    )
}

@Composable
internal fun AfternoteTypeContent(
    state: AfternoteEditorState,
    form: EditorFormState,
    onNavigateToMemorialPlaylist: () -> Unit,
    onNavigateToSelectReceiver: () -> Unit,
    onPhotoAddClick: () -> Unit,
    onVideoAddClick: () -> Unit,
    onAudioAddClick: () -> Unit,
    onThumbnailBytesReady: (ByteArray?) -> Unit,
    onThumbnailExtractionFailed: (Throwable) -> Unit,
    thumbnailRetryToken: Int,
) {
    when (form.selectedType) {
        AfternoteType.MEMORIAL -> {
            val displayedVideo = form.displayedMemorialVideo
            MemorialEditorContent(
                displayMemorialPhotoUri = form.displayMemorialPhotoUri(),
                playlistAlbumCovers = form.displayAlbumCovers(),
                memorialVideoUrl = displayedVideo?.url,
                memorialThumbnailUrl = displayedVideo?.thumbnailUrl,
                memorialAudioUrl = form.memorialAudioUrl,
                editorMessages = state.editorMessages,
                onMessageRegisterClick = state::registerEditorMessage,
                onMessageDeleteClick = state::removeEditorMessage,
                onMessageAddClick = state::addEditorMessage,
                recipientSection =
                    AfternoteEditorReceiverSection(
                        afternoteEditReceivers = form.afternoteEditReceivers,
                        onAddClick = onNavigateToSelectReceiver,
                        onItemDeleteClick = state.deleteReceiver,
                    ),
                onSongAddClick = onNavigateToMemorialPlaylist,
                onPhotoAddClick = onPhotoAddClick,
                onVideoAddClick = onVideoAddClick,
                onAudioAddClick = onAudioAddClick,
                onThumbnailBytesReady = onThumbnailBytesReady,
                onThumbnailExtractionFailed = onThumbnailExtractionFailed,
                thumbnailRetryToken = thumbnailRetryToken,
            )
        }

        AfternoteType.GALLERY_AND_FILES -> {
            GalleryAndFileEditorContent(
                editorMessages = state.editorMessages,
                onMessageRegisterClick = state::registerEditorMessage,
                onMessageDeleteClick = state::removeEditorMessage,
                onMessageAddClick = state::addEditorMessage,
                recipientSection =
                    AfternoteEditorReceiverSection(
                        afternoteEditReceivers = form.afternoteEditReceivers,
                        onAddClick = onNavigateToSelectReceiver,
                        onItemDeleteClick = state.deleteReceiver,
                    ),
                processingMethodSection =
                    ProcessingMethodSection(
                        items = form.processingMethods,
                        onItemDeleteClick = state.deleteProcessingMethod,
                        onItemAdded = state.addProcessingMethod,
                        onItemEdited = state.editProcessingMethod,
                    ),
            )
        }

        // ESTATE 는 디자인 미확정. 입력 자리를 비워 두고 placeholder 만 노출한다 (이슈 #195).
        AfternoteType.ESTATE -> {
            UnimplementedTypeContent()
        }

        // BUSINESS(시안 700:38735)는 SOCIAL 과 폼 구조가 동일(계정 정보* + 수신자 지정* + 처리 방법 리스트* + 남기실 말씀)해
        // AccountEditorContent 를 그대로 재사용한다 (이슈 #467).
        AfternoteType.SOCIAL_NETWORK, AfternoteType.BUSINESS -> {
            AccountEditorContent(
                editorMessages = state.editorMessages,
                onMessageRegisterClick = state::registerEditorMessage,
                onMessageDeleteClick = state::removeEditorMessage,
                onMessageAddClick = state::addEditorMessage,
                accountSection =
                    AccountSection(
                        idState = state.idState,
                        passwordState = state.passwordState,
                    ),
                recipientSection =
                    AfternoteEditorReceiverSection(
                        afternoteEditReceivers = form.afternoteEditReceivers,
                        onAddClick = onNavigateToSelectReceiver,
                        onItemDeleteClick = state.deleteReceiver,
                    ),
                processingMethodSection =
                    ProcessingMethodSection(
                        items = form.processingMethods,
                        onItemDeleteClick = state.deleteProcessingMethod,
                        onItemAdded = state.addProcessingMethod,
                        onItemEdited = state.editProcessingMethod,
                    ),
            )
        }
    }
}
