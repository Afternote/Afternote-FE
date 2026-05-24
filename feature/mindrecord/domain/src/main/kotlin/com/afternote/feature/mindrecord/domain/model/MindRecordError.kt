package com.afternote.feature.mindrecord.domain.model

/**
 * 도메인 레이어가 던지는 알려진 에러. 메시지 문자열을 도메인에 두지 않기 위해 typed
 * 형태로 정의하고, presentation 레이어가 R.string 으로 매핑한다.
 */
sealed class MindRecordError : Exception() {
    /** 카테고리 이름이 trim 후 비어 있을 때. */
    object EmptyCategoryName : MindRecordError()
}
