package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.author.editor.gallery.GalleryAndFileEditorContent
import com.afternote.feature.afternote.presentation.author.editor.gallery.GalleryAndFileEditorContentParams
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialGuidelineEditorContent
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialGuidelineEditorContentParams
import com.afternote.feature.afternote.presentation.author.editor.model.AccountSection
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverCallbacks
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.selection.DropdownMenuStyle
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdown
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdownLabelParams
import com.afternote.feature.afternote.presentation.author.editor.social.SocialNetworkEditorContent
import com.afternote.feature.afternote.presentation.author.editor.social.SocialNetworkEditorContentParams

@Composable
internal fun EditContent(
    state: AfternoteEditorState,
    onNavigateToAddSong: () -> Unit,
    onNavigateToSelectReceiver: () -> Unit,
    onPhotoAddClick: () -> Unit,
    onVideoAddClick: () -> Unit,
    onThumbnailBytesReady: (ByteArray?) -> Unit,
    bottomPadding: PaddingValues,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            SelectionDropdown(
                labelParams =
                    SelectionDropdownLabelParams(
                        label = "종류",
                    ),
                selectedValue = state.selectedCategory,
                options = state.categories,
                onValueSelected = state::onCategorySelected,
                menuStyle =
                    DropdownMenuStyle(
                        shadowElevation = 10.dp,
                        tonalElevation = 10.dp,
                    ),
                state = state.categoryDropdownState,
            )

            if (state.selectedCategory != CATEGORY_MEMORIAL_GUIDELINE) {
                Spacer(modifier = Modifier.height(16.dp))

                SelectionDropdown(
                    labelParams =
                        SelectionDropdownLabelParams(
                            label = "서비스명",
                        ),
                    selectedValue = state.selectedService,
                    options = state.currentServiceOptions,
                    onValueSelected = state::onServiceSelected,
                    menuStyle =
                        DropdownMenuStyle(
                            shadowElevation = 10.dp,
                            tonalElevation = 10.dp,
                        ),
                    state = state.serviceDropdownState,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            CategoryContent(
                state = state,
                onNavigateToAddSong = onNavigateToAddSong,
                onNavigateToSelectReceiver = onNavigateToSelectReceiver,
                onPhotoAddClick = onPhotoAddClick,
                onVideoAddClick = onVideoAddClick,
                onThumbnailBytesReady = onThumbnailBytesReady,
                bottomPadding = bottomPadding,
            )
        }
    }
}

@Composable
internal fun CategoryContent(
    state: AfternoteEditorState,
    onNavigateToAddSong: () -> Unit,
    onNavigateToSelectReceiver: () -> Unit,
    onPhotoAddClick: () -> Unit,
    onVideoAddClick: () -> Unit,
    onThumbnailBytesReady: (ByteArray?) -> Unit,
    bottomPadding: PaddingValues,
) {
    when (state.selectedCategory) {
        CATEGORY_MEMORIAL_GUIDELINE -> {
            MemorialGuidelineEditorContent(
                bottomPadding = bottomPadding,
                params =
                    MemorialGuidelineEditorContentParams(
                        displayMemorialPhotoUri = state.displayMemorialPhotoUri,
                        playlistSongCount = state.livePlaylistSongCount,
                        playlistAlbumCovers = state.displayAlbumCovers,
                        selectedLastWish = state.selectedLastWish,
                        lastWishOptions = state.lastWishOptions,
                        funeralVideoUrl = state.funeralVideoUrl,
                        funeralThumbnailUrl = state.funeralThumbnailUrl,
                        customLastWishText = state.customLastWishText,
                        recipientSection =
                            AfternoteEditorReceiverSection(
                                afternoteEditReceivers = state.afternoteEditReceivers,
                                callbacks =
                                    AfternoteEditorReceiverCallbacks(
                                        onAddClick = onNavigateToSelectReceiver,
                                        onItemDeleteClick = state::onAfternoteEditorReceiverDelete,
                                        onItemAdded = state::onAfternoteEditorReceiverItemAdded,
                                    ),
                            ),
                        onSongAddClick = onNavigateToAddSong,
                        onLastWishSelected = state::onLastWishSelected,
                        onCustomLastWishChanged = state::onCustomLastWishChanged,
                        onPhotoAddClick = onPhotoAddClick,
                        onVideoAddClick = onVideoAddClick,
                        onThumbnailBytesReady = onThumbnailBytesReady,
                    ),
            )
        }

        CATEGORY_GALLERY_AND_FILE -> {
            GalleryAndFileEditorContent(
                bottomPadding = bottomPadding,
                params =
                    GalleryAndFileEditorContentParams(
                        messageTitleState = state.messageTitleState,
                        messageState = state.messageState,
                        recipientSection =
                            AfternoteEditorReceiverSection(
                                afternoteEditReceivers = state.afternoteEditReceivers,
                                callbacks =
                                    AfternoteEditorReceiverCallbacks(
                                        onAddClick = onNavigateToSelectReceiver,
                                        onItemDeleteClick = state::onAfternoteEditorReceiverDelete,
                                        onItemAdded = state::onAfternoteEditorReceiverItemAdded,
                                    ),
                            ),
                        processingMethodSection =
                            ProcessingMethodSection(
                                items = state.galleryProcessingMethods,
                                callbacks = state.galleryProcessingCallbacks,
                            ),
                    ),
            )
        }

        else -> {
            SocialNetworkEditorContent(
                bottomPadding = bottomPadding,
                params =
                    SocialNetworkEditorContentParams(
                        messageTitleState = state.messageTitleState,
                        messageState = state.messageState,
                        accountSection =
                            AccountSection(
                                idState = state.idState,
                                passwordState = state.passwordState,
                                selectedMethod = state.selectedProcessingMethod,
                                onMethodSelected = state::onProcessingMethodSelected,
                            ),
                        recipientSection =
                            AfternoteEditorReceiverSection(
                                afternoteEditReceivers = state.afternoteEditReceivers,
                                callbacks =
                                    AfternoteEditorReceiverCallbacks(
                                        onAddClick = onNavigateToSelectReceiver,
                                        onItemDeleteClick = state::onAfternoteEditorReceiverDelete,
                                        onItemAdded = state::onAfternoteEditorReceiverItemAdded,
                                    ),
                            ),
                        processingMethodSection =
                            ProcessingMethodSection(
                                items = state.processingMethods,
                                callbacks = state.socialProcessingCallbacks,
                            ),
                    ),
            )
        }
    }
}
