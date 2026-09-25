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
 *
 * ### 임시저장은 «전부 건너뛰기» 가 아니다 (#808)
 *
 * 서버가 완화하는 것은 **카테고리 전략의 필수값(`credentials`·`playlist`)뿐**이다. 그 위의 검증은
 * `isDraft` 와 무관하게 그대로 돈다 — BE `AfternoteValidator.validateCreateRequest` 가
 * `validateLeaveMessage` 를 전략보다 **먼저** 부르고, `title` 은 `AfternoteCreateRequest` 의
 * `@NotBlank` 라 Bean Validation 단계에서 걸린다.
 *
 * | 검증 | 임시저장 | 근거 |
 * |---|---|---|
 * | [AfternoteValidationError.UNIMPLEMENTED_TYPE] | **막는다** | 서버 이전 문제다. 통과시키면 `AfternoteEditorFormMapper` 의 `error(...)` 에 도달해 앱이 죽는다 |
 * | [AfternoteValidationError.TITLE_REQUIRED] | **막는다** | `AfternoteCreateRequest.title` 이 `@NotBlank` |
 * | [AfternoteValidationError.LEAVE_MESSAGE_BODY_REQUIRED] | **막는다** | `AfternoteValidationCommons.validateLeaveMessage` 가 본문 빈 블록에 400 |
 * | [AfternoteValidationError.ACCOUNT_CREDENTIALS_REQUIRED] | 건너뛴다 | 서버가 임시저장에서 완화하는 바로 그 축 |
 *
 * 막지 않으면 「임시저장을 눌렀는데 400 이 나고 작성분이 통째로 날아감」이 된다. 임시저장은
 * «미완성을 보존하는» 수단이므로 **서버가 받아 주는 범위 안에서만** 느슨해야 한다.
 */
internal object AfternoteEditorValidator {
    fun validate(
        form: EditorFormState,
        payload: RegisterAfternotePayload,
        asDraft: Boolean = false,
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
        // 여기서부터가 «정식 등록에서만» 막는 축이다 — 서버가 임시저장에서 완화하는 범위와 같다.
        if (!asDraft) {
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

                // ESTATE 는 위에서 이미 반환했다 — 여기 도달하지 않는다.
                AfternoteType.ESTATE -> {}
            }
        }
        return when (errors.size) {
            0 -> null
            1 -> errors.single()
            else -> AfternoteValidationError.MULTIPLE_REQUIRED_FIELDS
        }
    }
}
