package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.domain.repository.author.MediaInput
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 추모 영상 필드의 편집 상태 — 서버에 반영된 기준값과 이번 폼의 미저장 교체분을 한 개념으로 묶는다.
 *
 * 네 상태를 이름으로 갖는다: [Empty] · [ServerOnly] · [SelectionOnly] · [Replaced]. 종전에는 nullable 칸
 * 두 개의 조합으로 같은 넷을 표현했는데, 다섯 번째 상태(업로드 대기·실패 등)를 칸 하나 더 얹어
 * 표현하면 연산 넷과 파생값 둘 중 어느 것이 새 상태를 빠뜨렸는지 컴파일러가 알려 주지 않았다.
 * 지금은 `else` 없는 `when` 여섯 곳이 전부 컴파일 에러로 드러난다(#1901).
 *
 * 상태를 따로 드는 이유는 저장 출처다. 교체분이 있으면 업로드([MediaInput.Local]), 서버 기준값만
 * 있으면 유지([MediaInput.Remote]), 둘 다 없으면 명시적 `null`([MediaInput.None], BE 삭제 계약
 * #1596·#1597)로 갈리며, 이 판정을 URL 모양으로 재추론하지 않는다(#1406). 시트의 삭제는 두 층을 함께
 * 비운다 — 서버 영상 A 위에 로컬 B 를 골랐다가 지우면 A 로 돌아가지 않고 슬롯이 빈다. 저장 전까지 서버는
 * 그대로이고, 저장 없이 나가면 이탈 가드가 막는다. 두 값을 호출부에 노출하지 않고 표시·저장 규칙을
 * 여기서 완결한다.
 *
 * 직렬화 wire 형태가 달라졌다 — 종전 `{"persisted":…,"selection":…}` 대신 하위 타입 판별자가 붙은
 * `{"type":"replaced","persisted":…,"selection":…}` 다. 판별자 값은 `@SerialName` 으로 고정해
 * 패키지·클래스 이름 변경에 끌려가지 않게 했다. 스냅샷 키는 그래서 `v5` 로 올렸다
 * (`AfternoteEditorViewModel` 의 `EDITOR_FORM_SNAPSHOT_KEY`).
 *
 * 하위 타입이 `private` 이라 밖에서는 `when` 으로 분해할 수 없다 — 두 값은 이 파일을 벗어나지 않는다.
 * 종전 `private constructor` + `@ConsistentCopyVisibility` 가 지키던 경계와 같다.
 */
@Serializable
internal sealed class EditableMemorialVideo {
    @Serializable
    @SerialName("empty")
    private data object Empty : EditableMemorialVideo()

    @Serializable
    @SerialName("server_only")
    private data class ServerOnly(
        val persisted: MemorialVideoAttachment,
    ) : EditableMemorialVideo()

    @Serializable
    @SerialName("selection_only")
    private data class SelectionOnly(
        val selection: MemorialVideoAttachment,
    ) : EditableMemorialVideo()

    @Serializable
    @SerialName("replaced")
    private data class Replaced(
        val persisted: MemorialVideoAttachment,
        val selection: MemorialVideoAttachment,
    ) : EditableMemorialVideo()

    /** 화면에 표시하고 payload의 영상·썸네일 한 벌로 사용할 현재 값. */
    internal val displayed: MemorialVideoAttachment?
        get() =
            when (this) {
                Empty -> null
                is ServerOnly -> persisted
                is SelectionOnly -> selection
                is Replaced -> selection
            }

    /** 시트에 삭제 항목을 내놓을 수 있는지 — 표시된 층이 있으면 출처와 무관하게 지울 수 있다(#1597). */
    internal val canRemove: Boolean
        get() =
            when (this) {
                Empty -> false
                is ServerOnly -> true
                is SelectionOnly -> true
                is Replaced -> true
            }

    /** 새 선택으로 교체한다. 이전 교체분의 썸네일은 물려주지 않는다. 빈 문자열은 [MemorialVideoAttachment.ofOrNull] 규칙대로 첨부 없음이다. */
    internal fun withSelection(url: String): EditableMemorialVideo {
        val picked = MemorialVideoAttachment.ofOrNull(url)
        return when (this) {
            Empty -> of(persisted = null, selection = picked)
            is ServerOnly -> of(persisted = persisted, selection = picked)
            is SelectionOnly -> of(persisted = null, selection = picked)
            is Replaced -> of(persisted = persisted, selection = picked)
        }
    }

    /** 미저장 교체분에서 파생된 썸네일만 갱신한다. 교체분이 사라졌다면 늦은 결과를 버린다. */
    internal fun withSelectionThumbnail(url: String): EditableMemorialVideo =
        when (this) {
            Empty -> this
            is ServerOnly -> this
            is SelectionOnly -> SelectionOnly(selection.copy(thumbnailUrl = url))
            is Replaced -> Replaced(persisted, selection.copy(thumbnailUrl = url))
        }

    /**
     * 두 층에서 썸네일만 뗀 사본 — 이탈 가드 지문에 실린다. 왜 썸네일을 빼는지는
     * [MemorialVideoAttachment.withoutThumbnail], 왜 서버 기준값이 지문에 남아야 하는지는
     * [AfternoteTypeForm.Memorial.enteredContentOrNull] 이 말한다.
     */
    internal fun withoutThumbnail(): EditableMemorialVideo =
        when (this) {
            Empty -> this
            is ServerOnly -> ServerOnly(persisted.withoutThumbnail())
            is SelectionOnly -> SelectionOnly(selection.withoutThumbnail())
            is Replaced -> Replaced(persisted.withoutThumbnail(), selection.withoutThumbnail())
        }

    /** 출처를 URL 모양으로 재추론하지 않고 저장 경계의 명시적인 입력 타입으로 바꾼다. */
    internal fun toMediaInput(): MediaInput =
        when (this) {
            Empty -> MediaInput.None
            is ServerOnly -> MediaInput.Remote(persisted.url)
            is SelectionOnly -> MediaInput.Local(selection.url)
            is Replaced -> MediaInput.Local(selection.url)
        }

    internal companion object {
        internal fun empty(): EditableMemorialVideo = Empty

        internal fun fromPersisted(persisted: MemorialVideoAttachment?): EditableMemorialVideo = of(persisted = persisted, selection = null)

        /** 두 층의 유무 조합을 상태 이름으로 접는 유일한 자리. */
        private fun of(
            persisted: MemorialVideoAttachment?,
            selection: MemorialVideoAttachment?,
        ): EditableMemorialVideo =
            when {
                persisted != null && selection != null -> Replaced(persisted, selection)
                persisted != null -> ServerOnly(persisted)
                selection != null -> SelectionOnly(selection)
                else -> Empty
            }
    }
}
