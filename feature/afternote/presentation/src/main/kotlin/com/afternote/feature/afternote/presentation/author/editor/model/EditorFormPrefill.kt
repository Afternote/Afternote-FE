package com.afternote.feature.afternote.presentation.author.editor.model

import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

/**
 * ViewModel/Mapper가 [com.afternote.feature.afternote.domain.model.author.Detail] 등에서 조립해
 * [com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState.applyFormPrefill]에 넘기는 값 묶음.
 * 분기·enum 해석·메시지 파싱은 여기 이전 단계에서 끝난다.
 */
data class EditorFormPrefill(
    val loadedItemId: String,
    val serviceName: String,
    val category: EditorCategory,
    val accountId: String,
    val password: String,
    val messageBlocks: List<EditorMessageTextBlock>,
    val processingMethods: List<ProcessingMethodItem>,
    /**
     * 이 애프터노트에 지정된 수신자 (#566). 신규 작성 경로가 폼에 넣는
     * `AfternoteSaveState.authorReceivers`(= 작성자가 등록한 수신자 **전체**) 와 다른 값이다.
     */
    val receivers: List<AfternoteEditorReceiver>,
    val funeralVideoUrl: String?,
    val funeralThumbnailUrl: String?,
    val memorialPhotoUrl: String?,
    val memorialPlaylistSongs: List<Song> = emptyList(),
)
