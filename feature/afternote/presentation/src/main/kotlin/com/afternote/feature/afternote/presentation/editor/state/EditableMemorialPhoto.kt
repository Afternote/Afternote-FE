package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.domain.repository.author.MediaInput

/**
 * 영정사진의 서버 기준값과 미저장 선택. 표시·삭제·업로드 출처를 한 경계에서 결정한다.
 * 교체 전 서버 값은 이탈 가드와 기존 SavedStateHandle 스냅샷의 동일성을 위해 보존한다.
 * 삭제는 [empty]로 두 층을 함께 비우며, 서버 값으로 되돌리는 동작은 없다.
 */
internal sealed class EditableMemorialPhoto private constructor() {
    private data object Empty : EditableMemorialPhoto()

    private data class ServerOnly(
        val persisted: String,
    ) : EditableMemorialPhoto()

    private data class SelectionOnly(
        val selection: String,
    ) : EditableMemorialPhoto()

    private data class Replaced(
        val persisted: String,
        val selection: String,
    ) : EditableMemorialPhoto()

    internal val displayed: String?
        get() =
            when (this) {
                Empty -> null
                is ServerOnly -> persisted
                is SelectionOnly -> selection
                is Replaced -> selection
            }

    internal val canRemove: Boolean
        get() =
            when (this) {
                Empty -> false
                is ServerOnly -> persisted.isNotBlank()
                is SelectionOnly -> selection.isNotBlank()
                is Replaced -> selection.isNotBlank()
            }

    internal fun withSelection(uri: String): EditableMemorialPhoto =
        when (this) {
            Empty -> SelectionOnly(uri)
            is ServerOnly -> Replaced(persisted, uri)
            is SelectionOnly -> SelectionOnly(uri)
            is Replaced -> Replaced(persisted, uri)
        }

    /** 기존 빈 문자열 처리도 유지하며 URL 형태로 출처를 재추론하지 않는다. */
    internal fun toMediaInput(): MediaInput =
        when (this) {
            Empty -> MediaInput.None
            is ServerOnly -> persisted.asRemoteInput()
            is SelectionOnly -> if (selection.isBlank()) MediaInput.None else MediaInput.Local(selection)
            is Replaced -> if (selection.isBlank()) persisted.asRemoteInput() else MediaInput.Local(selection)
        }

    /** 기존 JSON의 두 키를 유지하는 직렬화 경계. UI와 저장 명령은 이 표현을 사용하지 않는다. */
    internal fun toSnapshot(): Snapshot =
        when (this) {
            Empty -> Snapshot(null, null)
            is ServerOnly -> Snapshot(persisted, null)
            is SelectionOnly -> Snapshot(null, selection)
            is Replaced -> Snapshot(persisted, selection)
        }

    internal data class Snapshot(
        val persisted: String?,
        val selection: String?,
    )

    internal companion object {
        internal fun empty(): EditableMemorialPhoto = Empty

        internal fun fromPersisted(uri: String?): EditableMemorialPhoto = if (uri == null) Empty else ServerOnly(uri)

        internal fun fromSnapshot(
            persisted: String?,
            selection: String?,
        ): EditableMemorialPhoto = fromPersisted(persisted).let { photo -> selection?.let(photo::withSelection) ?: photo }
    }
}

private fun String.asRemoteInput(): MediaInput = if (isBlank()) MediaInput.None else MediaInput.Remote(this)
