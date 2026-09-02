package com.afternote.feature.afternote.presentation.author.editor.state

import com.afternote.feature.afternote.domain.repository.author.MediaInput
import kotlinx.serialization.Serializable

/**
 * 추모 영상 필드의 편집 상태 — 서버에 반영된 기준값과 이번 폼의 미저장 교체분을 한 개념으로 묶는다.
 *
 * 화면에 보이는 영상은 하나지만, 서버 영상 A를 로컬 영상 B로 교체한 동안에는 둘을 함께 기억해야 한다.
 * B를 걷어내면 A로 돌아가야 하고, 현재 PATCH 계약은 `null`을 서버 영상 삭제가 아니라 기존 값 유지로
 * 해석하기 때문이다. 이 두 값을 호출부에 따로 노출하지 않고 표시·삭제·저장 규칙을 여기서 완결한다.
 */
@Serializable
internal class EditableMemorialVideo private constructor(
    private val persisted: MemorialVideoAttachment? = null,
    private val selection: MemorialVideoAttachment? = null,
) {
    /** 화면에 표시하고 payload의 영상·썸네일 한 벌로 사용할 현재 값. */
    internal val displayed: MemorialVideoAttachment? get() = selection ?: persisted

    /** 현재 화면에서 사용자의 새 선택을 걷어내는 동작을 제공할 수 있는지. */
    internal val canDiscardSelection: Boolean get() = selection != null

    /** 새 선택으로 교체한다. `null`이면 교체분만 걷어내고 서버 기준값으로 돌아간다. */
    internal fun withSelection(url: String?): EditableMemorialVideo =
        EditableMemorialVideo(
            persisted = persisted,
            selection = MemorialVideoAttachment.ofOrNull(url),
        )

    /** 미저장 교체분에서 파생된 썸네일만 갱신한다. 교체분이 사라졌다면 늦은 결과를 버린다. */
    internal fun withSelectionThumbnail(url: String?): EditableMemorialVideo =
        selection?.let {
            EditableMemorialVideo(
                persisted = persisted,
                selection = it.copy(thumbnailUrl = url),
            )
        } ?: this

    /** 이탈 가드에는 이번 폼에서 직접 고른 영상만 싣고 자동 파생 썸네일은 제외한다. */
    internal fun userEnteredPart(): EditableMemorialVideo = fromSelection(selection?.userEnteredPart())

    /** 출처를 URL 모양으로 재추론하지 않고 저장 경계의 명시적인 입력 타입으로 바꾼다. */
    internal fun toMediaInput(): MediaInput =
        when {
            selection != null -> MediaInput.Local(selection.url)
            persisted != null -> MediaInput.Remote(persisted.url)
            else -> MediaInput.None
        }

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is EditableMemorialVideo && persisted == other.persisted && selection == other.selection)

    override fun hashCode(): Int = 31 * (persisted?.hashCode() ?: 0) + (selection?.hashCode() ?: 0)

    override fun toString(): String = "EditableMemorialVideo(persisted=$persisted, selection=$selection)"

    internal companion object {
        internal fun empty(): EditableMemorialVideo = EditableMemorialVideo()

        internal fun fromPersisted(persisted: MemorialVideoAttachment?): EditableMemorialVideo =
            EditableMemorialVideo(persisted = persisted)

        private fun fromSelection(selection: MemorialVideoAttachment?): EditableMemorialVideo = EditableMemorialVideo(selection = selection)
    }
}
