package com.afternote.feature.afternote.presentation.editor.mapper

import androidx.annotation.StringRes
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R

/**
 * [AfternoteType]을 Edit 화면 카테고리 드롭다운용 문자열 리소스로 변환합니다.
 *
 * 타이틀(사용자 정의)로 카테고리를 추론하지 말고, 반드시 이 확장 프로퍼티로 도메인 타입을 매핑하세요.
 * Compose에서는 `stringResource(type.editScreenLabelRes)`로 해석합니다.
 */
val AfternoteType.editScreenLabelRes: Int
    @StringRes
    get() =
        when (this) {
            AfternoteType.SOCIAL_NETWORK -> R.string.afternote_editor_category_social
            AfternoteType.BUSINESS -> R.string.afternote_editor_category_business
            AfternoteType.GALLERY_AND_FILES -> R.string.afternote_editor_category_gallery
            AfternoteType.ESTATE -> R.string.afternote_editor_category_estate
            AfternoteType.MEMORIAL -> R.string.afternote_editor_category_memorial
        }

/**
 * 서비스 카탈로그 드롭다운을 제공하는 에디터 타입인지 여부.
 *
 * MEMORIAL은 서비스명 입력 없이 고정 제목을 사용한다. ESTATE는 에디터 미설계 상태라 현재 드롭다운을
 * 제공하지 않으며, 구현이 열리면 하위 항목 카탈로그와 함께 이 값을 갱신한다(#491).
 */
val AfternoteType.hasServiceSelection: Boolean
    get() =
        this == AfternoteType.SOCIAL_NETWORK ||
            this == AfternoteType.BUSINESS ||
            this == AfternoteType.GALLERY_AND_FILES
