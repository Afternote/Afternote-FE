package com.afternote.feature.afternote.presentation.author.editor

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.modifierextention.shimmerLoadingPlaceholder
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.account.AccountSection
import com.afternote.feature.afternote.presentation.author.editor.gallery.GalleryAndFileEditorContent
import com.afternote.feature.afternote.presentation.author.editor.gallery.GalleryAndFileEditorContentParams
import com.afternote.feature.afternote.presentation.author.editor.memorial.guideline.MemorialGuidelineEditorContent
import com.afternote.feature.afternote.presentation.author.editor.memorial.guideline.MemorialGuidelineEditorContentParams
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverCallbacks
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.author.editor.selection.DropdownMenuStyle
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdown
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdownLabelParams
import com.afternote.feature.afternote.presentation.author.editor.social.SocialNetworkEditorContent
import com.afternote.feature.afternote.presentation.author.editor.social.SocialNetworkEditorContentParams
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.author.editor.state.rememberAfternoteEditorState

@Composable
internal fun EditorContent(
    state: AfternoteEditorState,
    form: EditorFormState,
    graphSongs: List<Song>,
    modifier: Modifier = Modifier,
    isPrefillLoading: Boolean = false,
    onNavigateToAddSong: () -> Unit,
    onNavigateToSelectReceiver: () -> Unit,
    onPhotoAddClick: () -> Unit,
    onVideoAddClick: () -> Unit,
    onThumbnailBytesReady: (ByteArray?) -> Unit,
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

        SelectionDropdown(
            labelParams =
                SelectionDropdownLabelParams(
                    label = stringResource(R.string.afternote_editor_label_category),
                ),
            selectedValue = form.selectedCategory.toDropdownLabel(),
            options = editorCategoryDropdownLabels(),
            onValueSelected = state::onCategorySelected,
            expanded = state.categoryDropdownExpanded,
            onExpandedChange = state::onCategoryDropdownExpandedChange,
            menuStyle =
                DropdownMenuStyle(
                    shadowElevation = 10.dp,
                    tonalElevation = 10.dp,
                ),
        )

        if (isPrefillLoading) {
            EditorPrefillSkeleton(category = form.selectedCategory)
            return@Column
        }

        if (form.selectedCategory != EditorCategory.MEMORIAL) {
            Spacer(modifier = Modifier.height(20.dp))

            SelectionDropdown(
                labelParams =
                    SelectionDropdownLabelParams(
                        label = stringResource(R.string.afternote_editor_label_service_name),
                    ),
                selectedValue = form.selectedService,
                options = form.currentServiceOptions,
                onValueSelected = state::onServiceSelected,
                expanded = state.serviceDropdownExpanded,
                onExpandedChange = state::onServiceDropdownExpandedChange,
                menuStyle =
                    DropdownMenuStyle(
                        shadowElevation = 10.dp,
                        tonalElevation = 10.dp,
                    ),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        CategoryContent(
            state = state,
            form = form,
            graphSongs = graphSongs,
            onNavigateToAddSong = onNavigateToAddSong,
            onNavigateToSelectReceiver = onNavigateToSelectReceiver,
            onPhotoAddClick = onPhotoAddClick,
            onVideoAddClick = onVideoAddClick,
            onThumbnailBytesReady = onThumbnailBytesReady,
        )
    }
}

/**
 * 수정 모드 진입 시 `getDetail()` 응답이 도착하기 전까지 prefill 대상 섹션을 가리는 skeleton.
 * 카테고리 드롭다운은 navArg 로 즉시 채워지므로 본 컴포저블 위쪽에서 그대로 노출하고,
 * 본 컴포저블은 그 아래(서비스명·계정·처리 방법·메시지 등)를 카테고리별 대략적인 layout 으로 placeholder 처리한다.
 * [shimmerLoadingPlaceholder] 로 가벼운 shimmer 애니메이션을 적용.
 */
@Composable
private fun EditorPrefillSkeleton(
    category: EditorCategory,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(20.dp))

        if (category != EditorCategory.MEMORIAL) {
            // 서비스명 드롭다운 자리.
            SkeletonBar(height = 56.dp)
            Spacer(modifier = Modifier.height(32.dp))
        }

        when (category) {
            EditorCategory.MEMORIAL -> MemorialPrefillSkeleton()

            EditorCategory.GALLERY -> GalleryPrefillSkeleton()

            EditorCategory.SOCIAL -> SocialPrefillSkeleton()

            // placeholder 카테고리는 prefill 자리가 없으므로 skeleton 도 그리지 않는다.
            EditorCategory.BUSINESS, EditorCategory.ESTATE -> Unit
        }
    }
}

@Composable
private fun SocialPrefillSkeleton() {
    // 계정 ID/PW.
    SkeletonBar(height = 56.dp)
    Spacer(modifier = Modifier.height(12.dp))
    SkeletonBar(height = 56.dp)
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
    // 추모 사진.
    SkeletonBar(height = 180.dp)
    Spacer(modifier = Modifier.height(20.dp))
    // 추모 영상.
    SkeletonBar(height = 120.dp)
    Spacer(modifier = Modifier.height(20.dp))
    // 추억 플레이리스트.
    SkeletonBar(height = 96.dp)
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
internal fun CategoryContent(
    state: AfternoteEditorState,
    form: EditorFormState,
    graphSongs: List<Song>,
    onNavigateToAddSong: () -> Unit,
    onNavigateToSelectReceiver: () -> Unit,
    onPhotoAddClick: () -> Unit,
    onVideoAddClick: () -> Unit,
    onThumbnailBytesReady: (ByteArray?) -> Unit,
) {
    when (form.selectedCategory) {
        EditorCategory.MEMORIAL -> {
            MemorialGuidelineEditorContent(
                params =
                    MemorialGuidelineEditorContentParams(
                        displayMemorialPhotoUri = form.displayMemorialPhotoUri(),
                        playlistSongCount = form.livePlaylistSongCount(graphSongs),
                        playlistAlbumCovers = form.displayAlbumCovers(graphSongs),
                        selectedLastWish = form.selectedLastWish,
                        lastWishOptions = editorLastWishOptions(),
                        funeralVideoUrl = form.funeralVideoUrl,
                        funeralThumbnailUrl = form.funeralThumbnailUrl,
                        customLastWishState = state.customLastWishState,
                        recipientSection =
                            AfternoteEditorReceiverSection(
                                afternoteEditReceivers = form.afternoteEditReceivers,
                                callbacks =
                                    AfternoteEditorReceiverCallbacks(
                                        onAddClick = onNavigateToSelectReceiver,
                                        onItemDeleteClick = state::onAfternoteEditorReceiverDelete,
                                        onItemAdded = state::onAfternoteEditorReceiverItemAdded,
                                    ),
                            ),
                        onSongAddClick = onNavigateToAddSong,
                        onLastWishSelected = state::onLastWishSelected,
                        onPhotoAddClick = onPhotoAddClick,
                        onVideoAddClick = onVideoAddClick,
                        onThumbnailBytesReady = onThumbnailBytesReady,
                    ),
            )
        }

        EditorCategory.GALLERY -> {
            GalleryAndFileEditorContent(
                params =
                    GalleryAndFileEditorContentParams(
                        editorMessages = state.editorMessages,
                        onMessageRegisterClick = {},
                        onMessageDeleteClick = state::removeEditorMessage,
                        onMessageAddClick = state::addEditorMessage,
                        recipientSection =
                            AfternoteEditorReceiverSection(
                                afternoteEditReceivers = form.afternoteEditReceivers,
                                callbacks =
                                    AfternoteEditorReceiverCallbacks(
                                        onAddClick = state::showAddAfternoteEditorReceiverDialog,
                                        onItemDeleteClick = state::onAfternoteEditorReceiverDelete,
                                        onItemAdded = state::onAfternoteEditorReceiverItemAdded,
                                    ),
                            ),
                        processingMethodSection =
                            ProcessingMethodSection(
                                items = form.galleryProcessingMethods,
                                callbacks = state.galleryProcessingCallbacks,
                            ),
                    ),
            )
        }

        // BUSINESS · ESTATE 는 디자인 미확정. 입력 자리를 비워 두고 placeholder 만 노출한다 (이슈 #195).
        EditorCategory.BUSINESS, EditorCategory.ESTATE -> {
            UnimplementedCategoryContent()
        }

        EditorCategory.SOCIAL -> {
            SocialNetworkEditorContent(
                params =
                    SocialNetworkEditorContentParams(
                        editorMessages = state.editorMessages,
                        onMessageRegisterClick = {},
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
                                callbacks =
                                    AfternoteEditorReceiverCallbacks(
                                        onAddClick = onNavigateToSelectReceiver,
                                        onItemDeleteClick = state::onAfternoteEditorReceiverDelete,
                                        onItemAdded = state::onAfternoteEditorReceiverItemAdded,
                                    ),
                            ),
                        processingMethodSection =
                            ProcessingMethodSection(
                                items = form.socialProcessingMethods,
                                callbacks = state.socialProcessingCallbacks,
                            ),
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorContentSocialPreview() {
    AfternoteTheme {
        val state = rememberAfternoteEditorState()
        EditorContent(
            state = state,
            form = state.currentForm().copy(selectedCategory = EditorCategory.SOCIAL),
            graphSongs = emptyList(),
            onNavigateToAddSong = {},
            onNavigateToSelectReceiver = {},
            onPhotoAddClick = {},
            onVideoAddClick = {},
            onThumbnailBytesReady = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorContentGalleryPreview() {
    AfternoteTheme {
        val state = rememberAfternoteEditorState()
        EditorContent(
            state = state,
            form = state.currentForm().copy(selectedCategory = EditorCategory.GALLERY),
            graphSongs = emptyList(),
            onNavigateToAddSong = {},
            onNavigateToSelectReceiver = {},
            onPhotoAddClick = {},
            onVideoAddClick = {},
            onThumbnailBytesReady = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorContentMemorialPreview() {
    AfternoteTheme {
        val state = rememberAfternoteEditorState()
        EditorContent(
            state = state,
            form = state.currentForm().copy(selectedCategory = EditorCategory.MEMORIAL),
            graphSongs = emptyList(),
            onNavigateToAddSong = {},
            onNavigateToSelectReceiver = {},
            onPhotoAddClick = {},
            onVideoAddClick = {},
            onThumbnailBytesReady = {},
        )
    }
}
