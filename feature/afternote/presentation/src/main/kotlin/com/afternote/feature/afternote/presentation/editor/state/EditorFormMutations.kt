package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver

/**
 * 폼 갱신 규칙을 담은 순수 변환 함수 모음. SSOT 소유자(ViewModel)와 Preview 용 자체 SSOT가
 * 같은 규칙을 공유하기 위한 자리다 — 파사드는 이 함수들을 직접 호출하지 않고 소유자 콜백을 거친다.
 */
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

/** 사진 선택은 `picked` 층에 실린다. 서버 사진은 `photoUrl` 에 남아 삭제 한 번으로 되돌아갈 자리가 된다. */
internal fun EditorFormState.withMemorialPhoto(uri: String): EditorFormState = mapMemorial { it.copy(pickedPhotoUri = uri) }

/**
 * 시트의 사진 삭제(#1114). 표시된 층 하나만 걷는다 — 로컬 교체분이 있으면 그것만 비워 서버 사진이 다시
 * 보이고, 서버 사진만 있으면 서버 축을 비워 PATCH `null` 로 잇는다(#1597). 어느 층을 걷을지는 호출부가
 * 아니라 폼이 정한다 — 삭제 항목 하나가 두 층을 구분해 눌리지 않기 때문이다(#1717).
 */
internal fun EditorFormState.withMemorialPhotoRemoved(): EditorFormState =
    mapMemorial { form ->
        if (form.pickedPhotoUri != null) form.copy(pickedPhotoUri = null) else form.copy(photoUrl = null)
    }

/**
 * 영상 첨부는 통째로 갈린다 — 새 영상에는 썸네일이 아직 없고, 이전 영상의 썸네일을 물려주면 다른
 * 영상의 그림이 붙는다.
 */
internal fun EditorFormState.withMemorialVideo(url: String): EditorFormState =
    mapMemorial { form -> form.copy(video = form.video.withSelection(url)) }

/**
 * 시트의 영상 삭제(#1114). 현재 표시된 한 층만 걷는다 — 고른 영상이 있으면 서버 원본으로 돌아가고,
 * 서버 영상만 있으면 서버 축을 비운다. 후자는 #1596의 명시적 `null` 직렬화와 BE 삭제 계약으로 이어져
 * 저장 뒤 다시 살아나는 거짓 삭제를 만들지 않는다(#1597). 층 판정은 [EditableMemorialVideo.removeDisplayed] 다.
 */
internal fun EditorFormState.withMemorialVideoRemoved(): EditorFormState =
    mapMemorial { form -> form.copy(video = form.video.removeDisplayed()) }

/**
 * 고른 영상에서 뽑아 올린 썸네일을 붙인다. 영상이 없으면 아무 일도 하지 않는다 — 썸네일만 남는
 * 상태를 만들 수 없으므로, 삭제한 뒤 늦게 도착한 업로드 결과는 조용히 버려진다.
 */
internal fun EditorFormState.withMemorialThumbnail(url: String): EditorFormState =
    mapMemorial { form -> form.copy(video = form.video.withSelectionThumbnail(url)) }

internal fun EditorFormState.withMemorialPlaylistSongs(songs: List<Song>): EditorFormState = mapMemorial { it.copy(playlistSongs = songs) }

/**
 * 목록에 이미 있는 로컬 ID 중 최대값 + 1.
 *
 * `size + 1` 은 중간 항목을 지운 뒤 추가할 때 남은 ID와 겹친다 — 겹치면 삭제·수정이 두 항목에 함께 걸린다.
 */
private fun nextLocalId(existing: List<Int>): Int = (existing.maxOrNull() ?: 0) + 1

internal fun EditorFormState.withReceiverDeleted(receiverId: Long): EditorFormState =
    copy(afternoteEditReceivers = afternoteEditReceivers.filter { it.id != receiverId })

/** 수신자 선택 화면에서 돌아온 결과 반영 — 이미 담긴 수신자면 그대로 둔다. */
internal fun EditorFormState.withReceiverAddedIfAbsent(
    receiverId: Long,
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

internal fun EditorFormState.withPrefillApplied(prefill: EditorFormPrefill): EditorFormState =
    copy(
        afternoteEditReceivers = prefill.receivers,
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

internal fun EditorFormState.withProcessingMethodsInitialized(methods: List<String>): EditorFormState =
    mapServiceForm { form ->
        if (form.processingMethods.isNotEmpty()) return@mapServiceForm form
        form.withProcessingMethods(
            methods.mapIndexed { index, text ->
                ProcessingMethodItem(localId = index + 1, text = text)
            },
        )
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
