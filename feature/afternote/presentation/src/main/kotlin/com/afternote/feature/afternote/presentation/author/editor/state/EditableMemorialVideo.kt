package com.afternote.feature.afternote.presentation.author.editor.state

import com.afternote.feature.afternote.domain.repository.author.MediaInput
import kotlinx.serialization.Serializable

/**
 * 추모 영상 필드의 편집 상태 — 서버에 반영된 기준값과 이번 폼의 미저장 교체분을 한 개념으로 묶는다.
 *
 * 화면에 보이는 영상은 하나지만, 서버 영상 A를 로컬 영상 B로 교체한 동안에는 둘을 함께 기억해야 한다.
 * B를 걷어내면 A로 돌아가야 하기 때문이다. 삭제는 표시된 층을 하나씩 걷는다 — 교체분이 있으면 그것만,
 * 서버 기준값만 있으면 그것을 비워 저장 시 명시적 `null`(BE 삭제 계약, #1596·#1597)로 잇는다.
 * 이 두 값을 호출부에 따로 노출하지 않고 표시·삭제·저장 규칙을 여기서 완결한다.
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

    /** 이번 편집에서 고른 영상만 걷어낸다. 서버 기준값은 남아 표시가 그리로 돌아간다. */
    internal fun discardSelection(): EditableMemorialVideo = copy(selection = null)

    /**
     * 표시된 층 하나를 걷는다. 교체분이 있으면 그것만 걷어 서버 기준값으로 돌아가고, 서버 기준값만
     * 있으면 그것을 비운다 — 저장 시 [MediaInput.None]이 명시적 `null`로 나가 서버 영상이 실제로
     * 지워진다(#1597). 되돌아갈 서버 상태가 없는 생성 모드에서는 슬롯이 그냥 빈다.
     */
    internal fun removeDisplayed(): EditableMemorialVideo = if (selection != null) discardSelection() else copy(persisted = null)

    /** 미저장 교체분에서 파생된 썸네일만 갱신한다. 교체분이 사라졌다면 늦은 결과를 버린다. */
    internal fun withSelectionThumbnail(url: String): EditableMemorialVideo =
        selection?.let { copy(selection = it.copy(thumbnailUrl = url)) } ?: this

    /**
     * 이탈 가드 지문에 실을 조각. 서버 기준값도 남긴다 — 수정 진입 기준선과 비교해야 서버 원본 삭제가
     * 미저장 변경으로 잡힌다(#1597). 영상에서 자동 파생되는 썸네일만 양쪽에서 걷어낸다.
     */
    internal fun userEnteredPart(): EditableMemorialVideo =
        copy(persisted = persisted?.userEnteredPart(), selection = selection?.userEnteredPart())

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
