package com.afternote.feature.afternote.presentation.author.editor.message

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/**
 * 남기실 말씀 항목 하나를 나타내는 모델
 */
@Stable
class EditorMessage(
    val id: String = UUID.randomUUID().toString(),
    val titleState: TextFieldState = TextFieldState(),
    val contentState: TextFieldState = TextFieldState(),
    initialRegistered: Boolean = false,
) {
    /** "등록" 버튼으로 확정되어 읽기 전용 표시 블록으로 전환됐는지 여부 (서버 프리필 말씀도 등록 상태). */
    var isRegistered by mutableStateOf(initialRegistered)

    /** 등록된 블록의 본문 펼침 여부 — 휘발성 UI 상태라 폼 스냅샷에 저장하지 않는다. */
    var isExpanded by mutableStateOf(false)
}
