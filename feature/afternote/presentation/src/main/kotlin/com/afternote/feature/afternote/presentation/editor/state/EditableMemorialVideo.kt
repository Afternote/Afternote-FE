package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.domain.repository.author.MediaInput
import kotlinx.serialization.Serializable

/**
 * 추모 영상 필드의 편집 상태 — 서버에 반영된 기준값과 이번 폼의 미저장 교체분을 한 개념으로 묶는다.
 *
 * 두 값을 따로 드는 이유는 저장 출처다. 교체분이 있으면 업로드([MediaInput.Local]), 서버 기준값만
 * 있으면 유지([MediaInput.Remote]), 둘 다 없으면 명시적 `null`([MediaInput.None], BE 삭제 계약
 * #1596·#1597)로 갈리며, 이 판정을 URL 모양으로 재추론하지 않는다(#1406). 시트의 삭제는 두 층을 함께
 * 비운다 — 서버 영상 A 위에 로컬 B 를 골랐다가 지우면 A 로 돌아가지 않고 슬롯이 빈다. 저장 전까지 서버는
 * 그대로이고, 저장 없이 나가면 이탈 가드가 막는다. 이 두 값을 호출부에 따로 노출하지 않고 표시·저장
 * 규칙을 여기서 완결한다.
 *
 * 생성자·`copy`·`componentN` 은 전부 private 이라 두 값은 이 클래스 밖으로 꺼낼 수 없다.
 * [ConsistentCopyVisibility] 가 `copy` 를 생성자 가시성에 맞춘다.
 */
@Serializable
@ConsistentCopyVisibility
internal data class EditableMemorialVideo private constructor(
    private val persisted: MemorialVideoAttachment? = null,
    private val selection: MemorialVideoAttachment? = null,
) {
    /** 화면에 표시하고 payload의 영상·썸네일 한 벌로 사용할 현재 값. */
    internal val displayed: MemorialVideoAttachment? get() = selection ?: persisted

    /** 시트에 삭제 항목을 내놓을 수 있는지 — 표시된 층이 있으면 출처와 무관하게 지울 수 있다(#1597). */
    internal val canRemove: Boolean get() = displayed != null

    /** 새 선택으로 교체한다. 이전 교체분의 썸네일은 물려주지 않는다. 빈 문자열은 [MemorialVideoAttachment.ofOrNull] 규칙대로 첨부 없음이다. */
    internal fun withSelection(url: String): EditableMemorialVideo = copy(selection = MemorialVideoAttachment.ofOrNull(url))

    /** 미저장 교체분에서 파생된 썸네일만 갱신한다. 교체분이 사라졌다면 늦은 결과를 버린다. */
    internal fun withSelectionThumbnail(url: String): EditableMemorialVideo =
        selection?.let { copy(selection = it.copy(thumbnailUrl = url)) } ?: this

    /**
     * 두 층에서 썸네일만 뗀 사본 — 이탈 가드 지문에 실린다. 왜 썸네일을 빼는지는
     * [MemorialVideoAttachment.withoutThumbnail], 왜 서버 기준값이 지문에 남아야 하는지는
     * [AfternoteTypeForm.Memorial.enteredContentOrNull] 이 말한다.
     */
    internal fun withoutThumbnail(): EditableMemorialVideo =
        copy(persisted = persisted?.withoutThumbnail(), selection = selection?.withoutThumbnail())

    /** 출처를 URL 모양으로 재추론하지 않고 저장 경계의 명시적인 입력 타입으로 바꾼다. */
    internal fun toMediaInput(): MediaInput =
        when {
            selection != null -> MediaInput.Local(selection.url)
            persisted != null -> MediaInput.Remote(persisted.url)
            else -> MediaInput.None
        }

    internal companion object {
        internal fun empty(): EditableMemorialVideo = EditableMemorialVideo()

        internal fun fromPersisted(persisted: MemorialVideoAttachment?): EditableMemorialVideo =
            EditableMemorialVideo(persisted = persisted)
    }
}
