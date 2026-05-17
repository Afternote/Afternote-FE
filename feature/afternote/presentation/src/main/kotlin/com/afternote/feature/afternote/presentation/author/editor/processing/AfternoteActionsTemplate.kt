package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.processing.AfternoteActionsTemplate.defaultsFor

/**
 * 카테고리별 추천 처리 방법(actions) 템플릿. 에디터 진입 시 prefill 용도.
 *
 * 사용자는 항목을 자유롭게 수정·삭제·추가할 수 있고, 저장 시 현재 값이 그대로 서버 `actions` 로 전송된다.
 * 서버는 예시 생성 로직이 없으므로 클라이언트가 책임진다.
 *
 * [defaultsFor] 는 `stringRes` ID 리스트를 반환하며, 호출처(@Composable)에서 `stringResource(id)` 로 i18n 해석한다.
 */
object AfternoteActionsTemplate {
    @StringRes
    fun defaultsFor(category: EditorCategory): List<Int> =
        when (category) {
            EditorCategory.SOCIAL -> {
                listOf(
                    R.string.afternote_editor_actions_social_remove_post,
                    R.string.afternote_editor_actions_social_post_memorial,
                    R.string.afternote_editor_actions_social_switch_memorial_account,
                )
            }

            // BUSINESS(메일) 카테고리 — 디자인 액션 템플릿 미확정. 일단 SOCIAL 과 동일 prefill.
            EditorCategory.BUSINESS -> {
                listOf(
                    R.string.afternote_editor_actions_social_remove_post,
                    R.string.afternote_editor_actions_social_post_memorial,
                    R.string.afternote_editor_actions_social_switch_memorial_account,
                )
            }

            EditorCategory.GALLERY -> {
                listOf(
                    R.string.afternote_editor_actions_gallery_send_folder,
                    R.string.afternote_editor_actions_gallery_delete_folder,
                )
            }

            // ESTATE(재산 처리) — 이슈 #195 "제목만 구현" 정의에 따라 비어 있음. 디자인 확정 후 prefill 추가.
            EditorCategory.ESTATE -> {
                emptyList()
            }

            EditorCategory.MEMORIAL -> {
                emptyList()
            }
        }
}
