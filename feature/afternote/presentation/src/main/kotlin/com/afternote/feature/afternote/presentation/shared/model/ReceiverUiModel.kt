package com.afternote.feature.afternote.presentation.shared.model

import androidx.compose.runtime.Immutable

/**
 * 애프터노트 상세 화면에서 수신자 정보를 표시하기 위한 공용 UI 모델.
 *
 * Editor 화면의 `AfternoteEditorReceiver` 와는 목적이 분리되어 있으며,
 * 상세(Detail) 계열 화면(Gallery/SocialNetwork/MemorialGuideline)에서 공통으로 사용한다.
 *
 * @param id 리스트 구분용 식별자
 * @param name 수신자 이름
 * @param label 수신자 라벨(관계, 역할 등)
 */
@Immutable
data class ReceiverUiModel(
    val id: String,
    val name: String,
    val label: String,
)
