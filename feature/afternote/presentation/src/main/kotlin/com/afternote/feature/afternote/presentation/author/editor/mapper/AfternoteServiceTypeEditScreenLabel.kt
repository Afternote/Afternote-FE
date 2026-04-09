package com.afternote.feature.afternote.presentation.author.editor.mapper

import androidx.annotation.StringRes
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.R

/**
 * [AfternoteServiceType]을 Edit 화면 카테고리 드롭다운용 문자열 리소스로 변환합니다.
 *
 * 타이틀(사용자 정의)로 카테고리를 추론하지 말고, 반드시 이 확장 프로퍼티로 도메인 타입을 매핑하세요.
 * Compose에서는 `stringResource(type.editScreenLabelRes)`로 해석합니다.
 */
val AfternoteServiceType.editScreenLabelRes: Int
    @StringRes
    get() =
        when (this) {
            AfternoteServiceType.SOCIAL_NETWORK -> R.string.afternote_editor_category_social
            AfternoteServiceType.GALLERY_AND_FILES -> R.string.afternote_editor_category_gallery
            AfternoteServiceType.MEMORIAL -> R.string.afternote_editor_category_memorial
        }
