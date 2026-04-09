package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.AfternoteServiceType

/**
 * Presentation helpers for afternote list/edit UI (category labels, etc.).
 * List row construction from dummy data lives on [com.afternote.feature.afternote.presentation.author.editor.provider.FakeAfternoteEditorDataProvider].
 */
object AfternoteItemMapper {
    /**
     * Edit screen category dropdown value from [AfternoteServiceType].
     * Prefer this when type is known; do not infer category from title (titles are user-defined).
     */
    fun categoryStringForEditScreen(serviceType: AfternoteServiceType): String =
        when (serviceType) {
            AfternoteServiceType.SOCIAL_NETWORK -> "소셜네트워크"
            AfternoteServiceType.GALLERY_AND_FILES -> "갤러리 및 파일"
            AfternoteServiceType.MEMORIAL -> "추모 가이드라인"
        }
}
