package com.afternote.feature.afternote.presentation.author.editor.state

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

/**
 * 폼 갱신 규칙을 담은 순수 변환 함수 모음. SSOT 소유자(ViewModel)와 Preview 용 자체 SSOT가
 * 같은 규칙을 공유하기 위한 자리다 — 파사드는 이 함수들을 직접 호출하지 않고 소유자 콜백을 거친다.
 */
internal fun normalizeEditorMessageBlocks(blocks: List<EditorMessageTextBlock>): List<EditorMessageTextBlock> =
    blocks.ifEmpty { DEFAULT_EDITOR_MESSAGE_BLOCKS }

private inline fun EditorFormState.mapMemorial(block: (AfternoteTypeForm.Memorial) -> AfternoteTypeForm.Memorial): EditorFormState {
    val memorial = typeForm as? AfternoteTypeForm.Memorial ?: return this
    return copy(typeForm = block(memorial))
}

private inline fun EditorFormState.mapServiceForm(
    block: (AfternoteTypeForm.WithServiceAndProcessingMethods) -> AfternoteTypeForm.WithServiceAndProcessingMethods,
): EditorFormState {
    val serviceForm = typeForm as? AfternoteTypeForm.WithServiceAndProcessingMethods ?: return this
    return copy(typeForm = block(serviceForm))
}

/**
 * 같은 카테고리를 다시 고르면 아무것도 하지 않는다. 드롭다운 재선택뿐 아니라 프로세스 데스 복원 후
 * `LaunchedEffect(route.initialType)` 재발화가 이 경로를 타므로, 가드가 없으면 복원된 입력이 지워진다.
 */
internal fun EditorFormState.withType(type: AfternoteType): EditorFormState =
    if (typeForm.type == type) this else copy(typeForm = AfternoteTypeForm.pristineFor(type))

internal fun EditorFormState.withService(service: String): EditorFormState = mapServiceForm { it.withService(service) }

internal fun EditorFormState.withMemorialPhoto(uri: String?): EditorFormState = mapMemorial { it.copy(pickedPhotoUri = uri) }

/** 영상이 바뀌면 그 영상에서 뽑은 썸네일은 무효가 되므로 함께 비운다. */
internal fun EditorFormState.withMemorialVideo(url: String?): EditorFormState = mapMemorial { it.copy(videoUrl = url, thumbnailUrl = null) }

internal fun EditorFormState.withMemorialThumbnail(dataUrl: String?): EditorFormState = mapMemorial { it.copy(thumbnailUrl = dataUrl) }

internal fun EditorFormState.withMemorialPlaylistSongs(songs: List<Song>): EditorFormState = mapMemorial { it.copy(playlistSongs = songs) }

/**
 * 목록에 이미 있는 로컬 ID 중 최대값 + 1.
 *
 * `size + 1` 은 중간 항목을 지운 뒤 추가할 때 남은 ID와 겹친다 — 겹치면 삭제·수정이 두 항목에 함께 걸린다.
 */
private fun nextLocalId(existing: List<Int>): Int = (existing.maxOrNull() ?: 0) + 1

internal fun EditorFormState.withReceiverDeleted(receiverId: String): EditorFormState =
    copy(afternoteEditReceivers = afternoteEditReceivers.filter { it.id != receiverId })

/** 수신자 선택 화면에서 돌아온 결과 반영 — 이미 담긴 수신자면 그대로 둔다. */
internal fun EditorFormState.withReceiverAddedIfAbsent(
    receiverId: String,
    name: String,
    label: String,
): EditorFormState {
    if (afternoteEditReceivers.any { it.id == receiverId }) return this
    val next = AfternoteEditorReceiver(id = receiverId, name = name, label = label)
    return copy(afternoteEditReceivers = afternoteEditReceivers + next)
}

/** 신규 작성 진입 시 기본값 채움 — 이미 사용자가 담은 수신자가 있으면 덮지 않는다. */
internal fun EditorFormState.withReceiversReplacedIfEmpty(receivers: List<AfternoteEditorReceiver>): EditorFormState {
    if (receivers.isEmpty() || afternoteEditReceivers.isNotEmpty()) return this
    return copy(afternoteEditReceivers = receivers)
}

internal fun EditorFormState.withLeaveMessageBlocks(blocks: List<EditorMessageTextBlock>): EditorFormState =
    copy(leaveMessageBlocks = normalizeEditorMessageBlocks(blocks))

internal fun EditorFormState.withPrefillApplied(prefill: EditorFormPrefill): EditorFormState =
    copy(
        afternoteEditReceivers = prefill.receivers,
        leaveMessageBlocks = normalizeEditorMessageBlocks(prefill.leaveMessageBlocks),
        typeForm = AfternoteTypeForm.fromPrefill(prefill.content),
    )

internal fun EditorFormState.withProcessingMethodAdded(text: String): EditorFormState =
    mapServiceForm { form ->
        val newItem =
            ProcessingMethodItem(
                localId = nextLocalId(form.processingMethods.map { it.localId }),
                text = text,
            )
        form.withProcessingMethods(form.processingMethods + newItem)
    }

internal fun EditorFormState.withProcessingMethodDeleted(localId: Int): EditorFormState =
    mapServiceForm { form -> form.withProcessingMethods(form.processingMethods.filter { it.localId != localId }) }

internal fun EditorFormState.withProcessingMethodEdited(
    localId: Int,
    newText: String,
): EditorFormState =
    mapServiceForm { form ->
        form.withProcessingMethods(
            form.processingMethods.map { item ->
                if (item.localId == localId) item.copy(text = newText) else item
            },
        )
    }
