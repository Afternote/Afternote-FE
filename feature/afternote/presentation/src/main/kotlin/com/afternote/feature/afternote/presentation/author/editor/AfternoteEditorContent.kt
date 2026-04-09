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
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.InfoMethodSection
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.selection.DropdownMenuStyle
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdown
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdownLabelParams
import com.afternote.feature.afternote.presentation.author.editor.social.SocialNetworkEditorContent
import com.afternote.feature.afternote.presentation.author.editor.social.SocialNetworkEditorContentParams
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState

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
                selectedValue = state.selectedCategory.displayLabel,
                options = state.categories,
                onValueSelected = state::onCategorySelected,
                menuStyle =
                    DropdownMenuStyle(
                        shadowElevation = 10.dp,
                        tonalElevation = 10.dp,
                    ),
                state = state.categoryDropdownState,
            )

            if (state.selectedCategory != EditorCategory.MEMORIAL) {
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
        EditorCategory.MEMORIAL -> {
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
                        customLastWishState = state.customLastWishState,
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
                        onPhotoAddClick = onPhotoAddClick,
                        onVideoAddClick = onVideoAddClick,
                        onThumbnailBytesReady = onThumbnailBytesReady,
                    ),
            )
        }

        EditorCategory.GALLERY -> {
            GalleryAndFileEditorContent(
                bottomPadding = bottomPadding,
                params =
                    GalleryAndFileEditorContentParams(
                        editorMessages = state.editorMessages,
                        onMessageRegisterClick = {},
                        onMessageDeleteClick = state::removeEditorMessage,
                        onMessageAddClick = state::addEditorMessage,
                        infoMethodSection =
                            InfoMethodSection(
                                selectedMethod = state.selectedInformationProcessingMethod,
                                onMethodSelected = state::onInformationProcessingMethodSelected,
                            ),
                        recipientSection =
                            AfternoteEditorReceiverSection(
                                afternoteEditReceivers = state.afternoteEditReceivers,
                                callbacks =
                                    AfternoteEditorReceiverCallbacks(
                                        onAddClick = state::showAddAfternoteEditorReceiverDialog,
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
                        editorMessages = state.editorMessages,
                        onMessageRegisterClick = {},
                        onMessageDeleteClick = state::removeEditorMessage,
                        onMessageAddClick = state::addEditorMessage,
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
