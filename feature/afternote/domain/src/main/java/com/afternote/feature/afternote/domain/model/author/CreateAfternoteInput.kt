package com.afternote.feature.afternote.domain.model.author

sealed interface CreateAfternoteInput {
    data class Social(
        val payload: CreateAccountPayload,
    ) : CreateAfternoteInput

    /**
     * BUSINESS 카테고리 생성 입력. 서버 요청 바디가 SOCIAL 과 동일 스키마
     * (계정 정보·처리 방법·남기실 말씀)라 [CreateAccountPayload] 를 공유하고,
     * category 문자열만 data 계층에서 "BUSINESS" 로 매핑된다.
     */
    data class Business(
        val payload: CreateAccountPayload,
    ) : CreateAfternoteInput

    data class Gallery(
        val payload: CreateGalleryPayload,
    ) : CreateAfternoteInput

    data class Memorial(
        val payload: CreateMemorialPayload,
    ) : CreateAfternoteInput
}
