package com.afternote.feature.afternote.presentation.author.edit

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
import com.afternote.feature.afternote.presentation.author.edit.gallery.GalleryAndFileEditContent
import com.afternote.feature.afternote.presentation.author.edit.gallery.GalleryAndFileEditContentParams
import com.afternote.feature.afternote.presentation.author.edit.memorial.MemorialGuidelineEditContent
import com.afternote.feature.afternote.presentation.author.edit.memorial.MemorialGuidelineEditContentParams
import com.afternote.feature.afternote.presentation.author.edit.model.AccountSection
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiverCallbacks
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiverSection
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.edit.selection.DropdownMenuStyle
import com.afternote.feature.afternote.presentation.author.edit.selection.SelectionDropdown
import com.afternote.feature.afternote.presentation.author.edit.selection.SelectionDropdownLabelParams
import com.afternote.feature.afternote.presentation.author.edit.social.SocialNetworkEditContent
import com.afternote.feature.afternote.presentation.author.edit.social.SocialNetworkEditContentParams
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover

@Composable
internal fun EditContent(
    state: AfternoteEditState,
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
    state: AfternoteEditState,
    onNavigateToAddSong: () -> Unit,
    onNavigateToSelectReceiver: () -> Unit,
    onPhotoAddClick: () -> Unit,
    onVideoAddClick: () -> Unit,
    onThumbnailBytesReady: (ByteArray?) -> Unit,
    bottomPadding: PaddingValues,
) {
    when (state.selectedCategory) {
        CATEGORY_MEMORIAL_GUIDELINE -> {
            val albumCoversFromPlaylist =
                state.playlistStateHolder?.songs?.let { songs ->
                    songs.mapIndexed { _, s ->
                        AlbumCover(
                            id = s.id,
                            imageUrl = s.albumCoverUrl,
                            title = s.title,
                        )
                    }
                } ?: state.playlistAlbumCovers
            val livePlaylistSongCount =
                state.playlistStateHolder?.songs?.size ?: state.playlistSongCount
            MemorialGuidelineEditContent(
                bottomPadding = bottomPadding,
                params =
                    MemorialGuidelineEditContentParams(
                        displayMemorialPhotoUri =
                            state.pickedMemorialPhotoUri
                                ?: state.memorialPhotoUrl,
                        playlistSongCount = livePlaylistSongCount,
                        playlistAlbumCovers = albumCoversFromPlaylist,
                        selectedLastWish = state.selectedLastWish,
                        lastWishOptions = state.lastWishOptions,
                        funeralVideoUrl = state.funeralVideoUrl,
                        funeralThumbnailUrl = state.funeralThumbnailUrl,
                        customLastWishText = state.customLastWishText,
                        recipientSection =
                            AfternoteEditReceiverSection(
                                afternoteEditReceivers = state.afternoteEditReceivers,
                                callbacks =
                                    AfternoteEditReceiverCallbacks(
                                        onAddClick = onNavigateToSelectReceiver,
                                        onItemDeleteClick = state::onAfternoteEditReceiverDelete,
                                        onItemAdded = state::onAfternoteEditReceiverItemAdded,
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
            GalleryAndFileEditContent(
                bottomPadding = bottomPadding,
                params =
                    GalleryAndFileEditContentParams(
                        messageState = state.messageState,
                        recipientSection =
                            AfternoteEditReceiverSection(
                                afternoteEditReceivers = state.afternoteEditReceivers,
                                callbacks =
                                    AfternoteEditReceiverCallbacks(
                                        onAddClick = onNavigateToSelectReceiver,
                                        onItemDeleteClick = state::onAfternoteEditReceiverDelete,
                                        onItemAdded = state::onAfternoteEditReceiverItemAdded,
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
            SocialNetworkEditContent(
                bottomPadding = bottomPadding,
                params =
                    SocialNetworkEditContentParams(
                        messageState = state.messageState,
                        accountSection =
                            AccountSection(
                                idState = state.idState,
                                passwordState = state.passwordState,
                                selectedMethod = state.selectedProcessingMethod,
                                onMethodSelected = state::onProcessingMethodSelected,
                            ),
                        recipientSection =
                            AfternoteEditReceiverSection(
                                afternoteEditReceivers = state.afternoteEditReceivers,
                                callbacks =
                                    AfternoteEditReceiverCallbacks(
                                        onAddClick = onNavigateToSelectReceiver,
                                        onItemDeleteClick = state::onAfternoteEditReceiverDelete,
                                        onItemAdded = state::onAfternoteEditReceiverItemAdded,
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
