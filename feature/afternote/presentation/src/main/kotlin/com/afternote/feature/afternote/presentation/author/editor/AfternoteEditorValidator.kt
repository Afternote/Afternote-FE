package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
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
        playlistSongs: List<Song>,
    ): AfternoteValidationError? {
        // 미구현 placeholder 카테고리는 입력 상태와 무관하게 저장 불가 — 개별 필드 검증(수신자·서비스명)보다 먼저 차단해
        // "서비스명을 선택하라" 류의 그릴 수도 없는 필드에 대한 안내가 나가지 않게 한다.
        if (category == EditorCategory.ESTATE) {
            return AfternoteValidationError.UNIMPLEMENTED_CATEGORY
        }
        if (selectedReceiverIds.isEmpty()) {
            return AfternoteValidationError.RECEIVERS_REQUIRED
        }
        if (payload.serviceName.trim().isEmpty()) {
            return AfternoteValidationError.TITLE_REQUIRED
        }
        // 아무것도 안 쓴 빈 칸은 저장 시 버려지지만, 제목만 채운 블록은 서버가 400 으로 거절한다.
        if (payload.messageBlocks.any { it.title.isNotBlank() && it.body.isBlank() }) {
            return AfternoteValidationError.LEAVE_MESSAGE_BODY_REQUIRED
        }
        return when (category) {
            // BUSINESS 는 시안(700:38735) 필수 표기(계정 정보*, 처리 방법 리스트*)가 SOCIAL 과 동일해 같은 규칙을 쓴다.
            EditorCategory.SOCIAL, EditorCategory.BUSINESS -> validateAccount(payload)

            EditorCategory.GALLERY -> validateGallery(payload)

            EditorCategory.MEMORIAL -> validateMemorial(playlistSongs)

            // ESTATE 는 디자인 확정 전 placeholder 만 노출. UI 자체에서 입력이 막혀 있지만
            // 안전망으로 Validator 에서도 저장을 차단한다.
            EditorCategory.ESTATE -> AfternoteValidationError.UNIMPLEMENTED_CATEGORY
        }
    }

    private fun validateAccount(payload: RegisterAfternotePayload): AfternoteValidationError? {
        if (payload.accountId.isBlank() || payload.password.isBlank()) {
            return AfternoteValidationError.ACCOUNT_CREDENTIALS_REQUIRED
        }
        if (payload.processingMethods.isEmpty()) {
            return AfternoteValidationError.ACTIONS_REQUIRED
        }
        return null
    }

    private fun validateGallery(payload: RegisterAfternotePayload): AfternoteValidationError? {
        if (payload.processingMethods.isEmpty()) {
            return AfternoteValidationError.ACTIONS_REQUIRED
        }
        return null
    }

    private fun validateMemorial(playlistSongs: List<Song>): AfternoteValidationError? {
        if (playlistSongs.isEmpty()) {
            return AfternoteValidationError.PLAYLIST_SONGS_REQUIRED
        }
        return null
    }
}
