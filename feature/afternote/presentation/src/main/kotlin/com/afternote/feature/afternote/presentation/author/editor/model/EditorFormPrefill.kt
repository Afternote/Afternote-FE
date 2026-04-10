package com.afternote.feature.afternote.presentation.author.editor.model

import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem

/**
 * ViewModel/Mapper가 [com.afternote.feature.afternote.domain.model.Detail] 등에서 조립해
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
    /** null이면 계정 처리 방법 필드는 기존 값 유지 */
    val accountProcessingMethod: AccountProcessingMethod?,
    /** null이면 정보 처리 방법 필드는 기존 값 유지 */
    val informationProcessingMethod: InformationProcessingMethod?,
    val socialProcessingMethods: List<ProcessingMethodItem>,
    val galleryProcessingMethods: List<ProcessingMethodItem>,
    /** null이면 당부/직접입력 필드는 기존 값 유지 */
    val lastWishUpdate: LastWishPrefill?,
    val funeralVideoUrl: String?,
    val funeralThumbnailUrl: String?,
    val memorialPhotoUrl: String?,
)

/** "남기고 싶은 당부" UI 반영용 (이미 API 문자열과 기본 문구 매칭이 끝난 상태). */
data class LastWishPrefill(
    val selectedKey: String?,
    val customText: String,
)

/**
 * Preview·테스트용 중간 표현. [com.afternote.feature.afternote.presentation.author.editor.mapper.AfternoteEditorMapper.editorFormPrefillFromLoadParams]로
 * [EditorFormPrefill]로 변환한다.
 */
data class LoadFromExistingParams(
    val itemId: String,
    val serviceName: String,
    val categoryDisplayString: String,
    val account: LoadFromExistingAccountParams = LoadFromExistingAccountParams(),
    val processing: LoadFromExistingProcessingParams = LoadFromExistingProcessingParams(),
    val atmosphere: String? = null,
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
)

data class LoadFromExistingAccountParams(
    val id: String = "",
    val password: String = "",
)

data class LoadFromExistingProcessingParams(
    val message: String = "",
    val accountMethodName: String = "",
    val informationMethodName: String = "",
    val methods: List<ProcessingMethodItem> = emptyList(),
    val galleryMethods: List<ProcessingMethodItem> = emptyList(),
)
