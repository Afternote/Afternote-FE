package com.afternote.feature.afternote.presentation.shared

import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.R

/**
 * Tab categories for the shared 애프터노트 list screen (writer main and receiver list).
 *
 * [labelResId]는 UI에 표시할 문자열 리소스 ID이다.
 * [navKey]는 네비게이션 인자로 전달하는 안정적인 영문 키로, EditorCategory.name과 매핑된다.
 */
enum class AfternoteCategory(
    @StringRes val labelResId: Int,
    val navKey: String? = null,
) {
    ALL(R.string.afternote_category_all),
    SOCIAL_NETWORK(R.string.afternote_category_social_network, "SOCIAL"),
    GALLERY_AND_FILES(R.string.afternote_category_gallery_and_files, "GALLERY"),
    MEMORIAL(R.string.afternote_category_memorial, "MEMORIAL"),
}
