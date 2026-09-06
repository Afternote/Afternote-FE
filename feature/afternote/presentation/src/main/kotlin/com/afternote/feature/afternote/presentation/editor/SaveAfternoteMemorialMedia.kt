package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo

/** 저장 시 추억 노트 미디어 필드. 영상의 출처와 기준값은 [memorialVideo]가 보존한다. */
internal data class SaveAfternoteMemorialMedia(
    val memorialVideo: EditableMemorialVideo = EditableMemorialVideo.empty(),
    val memorialPhotoUrl: String? = null,
    val pickedMemorialPhotoUri: String? = null,
    /** 추모 음성 (#1118). 영상과 같이 로컬 URI 와 원격 URL 이 한 필드를 공유한다. */
    val memorialAudioUrl: String? = null,
)

/**
 * Android가 직접 열 수 있는 로컬 첨부 URI인지. 영상의 저장 출처 판정은 [EditableMemorialVideo]가
 * 담당하고, 이 함수는 [com.afternote.feature.afternote.presentation.editor.memorial.MemorialVideoUpload]의
 * 프레임 추출 가능 여부와 같은 key를 썼던 구 v3 스냅샷의 일회성 출처 이관에만 사용한다.
 */
internal fun String.isLocalContentUri(): Boolean = startsWith("content://")
