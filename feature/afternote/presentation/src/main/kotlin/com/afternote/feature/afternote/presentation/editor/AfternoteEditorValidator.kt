package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState

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
    ): AfternoteValidationError? {
        val type = form.selectedType
        // 미구현 placeholder 카테고리는 입력 상태와 무관하게 저장 불가 — 개별 필드 검증(서비스명 등)보다 먼저 차단해
        // "서비스명을 선택하라" 류의 그릴 수도 없는 필드에 대한 안내가 나가지 않게 한다.
        if (type == AfternoteType.ESTATE) {
            return AfternoteValidationError.UNIMPLEMENTED_TYPE
        }
        val errors = mutableListOf<AfternoteValidationError>()
        if (payload.serviceName.trim().isEmpty()) errors += AfternoteValidationError.TITLE_REQUIRED
        // 아무것도 안 쓴 빈 칸은 저장 시 버려지지만, 제목만 채운 블록은 서버가 400 으로 거절한다.
        if (payload.messageBlocks.any { it.title.isNotBlank() && it.body.isBlank() }) {
            errors += AfternoteValidationError.LEAVE_MESSAGE_BODY_REQUIRED
        }
        when (type) {
            // 필수 판정 기준은 서버 계약(정식 등록 타이트 검증)이다 — 계정형은 계정 정보만 서버가 요구하고,
            // 처리 방법(actions)·수신자는 서버도 「생략 또는 빈 배열 가능」인 선택 항목이다.
            AfternoteType.SOCIAL_NETWORK, AfternoteType.BUSINESS -> {
                if (payload.accountId.isBlank() || payload.password.isBlank()) {
                    errors += AfternoteValidationError.ACCOUNT_CREDENTIALS_REQUIRED
                }
            }

            AfternoteType.GALLERY_AND_FILES -> {}

            // 서버는 정식 등록 시 추모 플레이리스트 곡을 필수로 검증한다(PLAYLIST_SONGS_REQUIRED) —
            // 확정 안내 문구가 없어 FE 검증 신설은 #1389 에서 다룬다.
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
