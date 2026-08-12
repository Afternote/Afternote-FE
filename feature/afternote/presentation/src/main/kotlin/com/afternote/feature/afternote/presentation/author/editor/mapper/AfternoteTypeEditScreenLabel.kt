package com.afternote.feature.afternote.presentation.author.editor.mapper

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
