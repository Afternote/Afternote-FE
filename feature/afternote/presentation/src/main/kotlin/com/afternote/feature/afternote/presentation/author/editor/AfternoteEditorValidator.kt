package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.presentation.author.editor.account.AccountProcessMethod
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError

/**
 * 애프터노트 저장 전 필수 필드 검증.
 *
 * 카테고리별 필수 입력 조건을 검사하여 첫 번째 오류를 반환합니다.
 */
internal object AfternoteEditorValidator {
    fun validate(
        category: EditorCategory,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistStateHolder: MemorialPlaylistStateHolder?,
    ): AfternoteValidationError? {
        if (selectedReceiverIds.isEmpty()) {
            return AfternoteValidationError.RECEIVERS_REQUIRED
        }
        if (payload.serviceName.trim().isEmpty()) {
            return AfternoteValidationError.TITLE_REQUIRED
        }
        return when (category) {
            EditorCategory.SOCIAL -> validateSocial(payload)
            EditorCategory.GALLERY -> validateGallery(payload)
            EditorCategory.MEMORIAL -> validateMemorial(playlistStateHolder)
        }
    }

    private fun validateSocial(payload: RegisterAfternotePayload): AfternoteValidationError? {
        if (payload.accountId.isBlank() || payload.password.isBlank()) {
            return AfternoteValidationError.SOCIAL_CREDENTIALS_REQUIRED
        }
        if (payload.accountProcessingMethod !in AccountProcessMethod.validClientNames) {
            return AfternoteValidationError.SOCIAL_PROCESS_METHOD_REQUIRED
        }
        if (payload.processingMethods.isEmpty()) {
            return AfternoteValidationError.SOCIAL_ACTIONS_REQUIRED
        }
        return null
    }

    private fun validateGallery(payload: RegisterAfternotePayload): AfternoteValidationError? {
        if (payload.galleryProcessingMethods.isEmpty()) {
            return AfternoteValidationError.GALLERY_ACTIONS_REQUIRED
        }
        return null
    }

    private fun validateMemorial(playlistStateHolder: MemorialPlaylistStateHolder?): AfternoteValidationError? {
        if (playlistStateHolder == null || playlistStateHolder.songs.isEmpty()) {
            return AfternoteValidationError.PLAYLIST_SONGS_REQUIRED
        }
        return null
    }
}
