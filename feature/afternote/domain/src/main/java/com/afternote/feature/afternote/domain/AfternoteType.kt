package com.afternote.feature.afternote.domain

/**
 * 애프터노트 한 건의 종류.
 *
 * 개별 서비스(인스타그램·네이버 메일 등)는 이 축이 아니라
 * `AfternoteService` 카탈로그가 담는다.
 *
 * Shared by writer and receiver afternote lists; used for filtering and display (icon/label).
 */
enum class AfternoteType {
    SOCIAL_NETWORK,
    BUSINESS,
    GALLERY_AND_FILES,
    ESTATE,
    MEMORIAL,
}
