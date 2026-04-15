package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.account.AccountSection
import com.afternote.feature.afternote.presentation.author.editor.gallery.GalleryAndFileEditorContent
import com.afternote.feature.afternote.presentation.author.editor.gallery.GalleryAndFileEditorContentParams
import com.afternote.feature.afternote.presentation.author.editor.memorial.guideline.MemorialGuidelineEditorContent
import com.afternote.feature.afternote.presentation.author.editor.memorial.guideline.MemorialGuidelineEditorContentParams
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.InfoMethodSection
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
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteLightTheme

@Composable
internal fun EditContent(
    state: AfternoteEditorState,
    form: EditorFormState,
    modifier: Modifier = Modifier,
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
                .navigationBarsPadding()
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
            menuStyle =
                DropdownMenuStyle(
                    shadowElevation = 10.dp,
                    tonalElevation = 10.dp,
                ),
            state = state.categoryDropdownState,
        )

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
                menuStyle =
                    DropdownMenuStyle(
                        shadowElevation = 10.dp,
                        tonalElevation = 10.dp,
                    ),
                state = state.serviceDropdownState,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        CategoryContent(
            state = state,
            form = form,
            onNavigateToAddSong = onNavigateToAddSong,
            onNavigateToSelectReceiver = onNavigateToSelectReceiver,
            onPhotoAddClick = onPhotoAddClick,
            onVideoAddClick = onVideoAddClick,
            onThumbnailBytesReady = onThumbnailBytesReady,
        )
    }
}

@Composable
internal fun CategoryContent(
    state: AfternoteEditorState,
    form: EditorFormState,
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
                        playlistSongCount = form.livePlaylistSongCount(state.playlistStateHolder),
                        playlistAlbumCovers = form.displayAlbumCovers(state.playlistStateHolder),
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
                        infoMethodSection =
                            InfoMethodSection(
                                selectedMethod = form.selectedInformationProcessingMethod,
                                onMethodSelected = state::onInformationProcessingMethodSelected,
                            ),
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

        else -> {
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
                                selectedMethod = form.selectedProcessingMethod,
                                onMethodSelected = state::onProcessingMethodSelected,
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
private fun EditContentSocialPreview() {
    AfternoteLightTheme {
        val state = rememberAfternoteEditorState()
        val form by state.formState.collectAsStateWithLifecycle()
        EditContent(
            state = state,
            form = form,
            onNavigateToAddSong = {},
            onNavigateToSelectReceiver = {},
            onPhotoAddClick = {},
            onVideoAddClick = {},
            onThumbnailBytesReady = {},
        )
    }
}

@Preview(showBackground = true, name = "Gallery")
@Composable
private fun EditContentGalleryPreview() {
    AfternoteLightTheme {
        val state =
            rememberAfternoteEditorState().apply {
                onCategorySelected(EditorCategory.GALLERY.displayLabel)
            }
        val form by state.formState.collectAsStateWithLifecycle()
        EditContent(
            state = state,
            form = form,
            onNavigateToAddSong = {},
            onNavigateToSelectReceiver = {},
            onPhotoAddClick = {},
            onVideoAddClick = {},
            onThumbnailBytesReady = {},
        )
    }
}

@Preview(showBackground = true, name = "Memorial")
@Composable
private fun EditContentMemorialPreview() {
    AfternoteLightTheme {
        val state =
            rememberAfternoteEditorState().apply {
                onCategorySelected(EditorCategory.MEMORIAL.displayLabel)
            }
        val form by state.formState.collectAsStateWithLifecycle()
        EditContent(
            state = state,
            form = form,
            onNavigateToAddSong = {},
            onNavigateToSelectReceiver = {},
            onPhotoAddClick = {},
            onVideoAddClick = {},
            onThumbnailBytesReady = {},
        )
    }
}
