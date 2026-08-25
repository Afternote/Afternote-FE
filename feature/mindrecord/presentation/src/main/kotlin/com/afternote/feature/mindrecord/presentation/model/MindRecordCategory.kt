package com.afternote.feature.mindrecord.presentation.model

/**
 * 마음의 기록 카테고리.
 *
 * 표시용 아이콘·문구는 같은 패키지의 [MindRecordCategoryUi] 가 들고 있다.
 *
 * 이 타입을 아는 모듈은 홈·마음의 기록·app 셋뿐이라 core:model(11개 모듈이 의존)에 두지
 * 않는다. 셋 다 이미 이 모듈에 의존하고 있어 새 모듈 의존이 생기지 않는다 (#1085).
 */
enum class MindRecordCategory {
    DAILY_QUESTION,
    DIARY,
    WEEKLY_REPORT,
}
