package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState

/**
 * 애프터노트 저장 전 필수 필드 검증.
 *
 * 카테고리별 필수 입력 조건을 모두 검사한다. 누락이 하나면 해당 필드 오류를, 둘 이상이면
 * [AfternoteValidationError.MULTIPLE_REQUIRED_FIELDS]를 반환한다.
 */
internal object AfternoteEditorValidator {
    fun validate(
        form: EditorFormState,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
    ): AfternoteValidationError? {
        val type = form.selectedType
        // 미구현 placeholder 카테고리는 입력 상태와 무관하게 저장 불가 — 개별 필드 검증(수신자·서비스명)보다 먼저 차단해
        // "서비스명을 선택하라" 류의 그릴 수도 없는 필드에 대한 안내가 나가지 않게 한다.
        if (type == AfternoteType.ESTATE) {
            return AfternoteValidationError.UNIMPLEMENTED_TYPE
        }
        val errors = mutableListOf<AfternoteValidationError>()
        if (selectedReceiverIds.isEmpty()) errors += AfternoteValidationError.RECEIVERS_REQUIRED
        if (payload.serviceName.trim().isEmpty()) errors += AfternoteValidationError.TITLE_REQUIRED
        // 아무것도 안 쓴 빈 칸은 저장 시 버려지지만, 제목만 채운 블록은 서버가 400 으로 거절한다.
        if (payload.messageBlocks.any { it.title.isNotBlank() && it.body.isBlank() }) {
            errors += AfternoteValidationError.LEAVE_MESSAGE_BODY_REQUIRED
        }
        when (type) {
            // BUSINESS 는 시안(700:38735)의 필수 항목(계정 정보·처리 방법)이 SOCIAL 과 동일해 같은 규칙을 쓴다.
            AfternoteType.SOCIAL_NETWORK, AfternoteType.BUSINESS -> {
                if (payload.accountId.isBlank() || payload.password.isBlank()) {
                    errors += AfternoteValidationError.ACCOUNT_CREDENTIALS_REQUIRED
                }
                if (payload.processingMethods.isEmpty()) {
                    errors += AfternoteValidationError.PROCESSING_METHODS_REQUIRED
                }
            }

            AfternoteType.GALLERY_AND_FILES -> {
                if (payload.processingMethods.isEmpty()) {
                    errors += AfternoteValidationError.PROCESSING_METHODS_REQUIRED
                }
            }

            AfternoteType.MEMORIAL -> {}

            // ESTATE 는 디자인 확정 전 placeholder 만 노출. UI 자체에서 입력이 막혀 있지만
            // 안전망으로 Validator 에서도 저장을 차단한다.
            AfternoteType.ESTATE -> {}
        }
        return when (errors.size) {
            0 -> null
            1 -> errors.single()
            else -> AfternoteValidationError.MULTIPLE_REQUIRED_FIELDS
        }
    }
}
