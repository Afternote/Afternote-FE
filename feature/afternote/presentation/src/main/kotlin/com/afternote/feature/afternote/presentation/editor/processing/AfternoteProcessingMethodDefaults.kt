package com.afternote.feature.afternote.presentation.editor.processing

import androidx.annotation.StringRes
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.processing.AfternoteProcessingMethodDefaults.defaultsFor

/**
 * 카테고리별 추천 처리 방법 기본값. 에디터 진입 시 prefill 용도.
 *
 * 사용자는 항목을 자유롭게 수정·삭제·추가할 수 있고, 저장 시 현재 처리 방법 목록이 그대로 전송된다.
 * 서버는 예시 생성 로직이 없으므로 클라이언트가 책임진다.
 *
 * [defaultsFor] 는 `stringRes` ID 리스트를 반환하며, Compose UI 경계에서 현재 locale 문자열로 해석한다.
 */
object AfternoteProcessingMethodDefaults {
    @StringRes
    fun defaultsFor(type: AfternoteType): List<Int> =
        when (type) {
            AfternoteType.SOCIAL_NETWORK -> {
                listOf(
                    R.string.afternote_editor_processing_method_social_remove_post,
                    R.string.afternote_editor_processing_method_social_post_memorial,
                    R.string.afternote_editor_processing_method_social_switch_memorial_account,
                )
            }

            AfternoteType.GALLERY_AND_FILES -> {
                listOf(
                    R.string.afternote_editor_processing_method_gallery_send_folder,
                    R.string.afternote_editor_processing_method_gallery_delete_folder,
                )
            }

            AfternoteType.MEMORIAL -> {
                emptyList()
            }

            // BUSINESS 는 소셜 폼을 재사용하지만 템플릿 prefill 미연결 상태(호출처 없음)는 동일해 빈 목록 유지.
            // ESTATE 는 디자인 확정 전 placeholder 만 노출되므로 prefill 대상 아님.
            AfternoteType.BUSINESS, AfternoteType.ESTATE -> {
                emptyList()
            }
        }
}
