package com.afternote.feature.afternote.presentation.editor.processing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem

/**
 * ProcessingMethodList의 상태를 관리하는 State Holder
 */
@Stable
class ProcessingMethodListState(
    initialShowTextField: Boolean = false,
    initialExpandedLocalId: Int? = null,
) {
    var showTextField by mutableStateOf(initialShowTextField)
        private set

    var editingLocalId by mutableStateOf<Int?>(null)
        private set

    val expandedStates = mutableStateMapOf<Int, Boolean>()

    /** Stored from constructor for use when [initializeExpandedStates] is called with null. */
    private val defaultExpandedLocalId: Int? = initialExpandedLocalId

    /**
     * 현재 [items]에 맞춰 expanded 맵을 동기화한다.
     *
     * - 목록에서 사라진 로컬 ID의 expanded 항목은 제거한다.
     * - 편집 중이던 행이 목록에서 제거되면 편집 모드를 해제한다.
     * - **이미 존재하는 로컬 ID**의 expanded 값은 덮어쓰지 않는다(열림/드롭다운 상태 보존).
     * - **새로 나타난 로컬 ID**만 [initialExpandedLocalId]·[defaultExpandedLocalId] 기준으로 시드한다.
     */
    fun initializeExpandedStates(
        items: List<ProcessingMethodItem>,
        initialExpandedLocalId: Int?,
    ) {
        val localIds = items.mapTo(mutableSetOf()) { it.localId }
        expandedStates.keys.toList().forEach { localId ->
            if (localId !in localIds) {
                expandedStates.remove(localId)
            }
        }
        val editing = editingLocalId
        if (editing != null && editing !in localIds) {
            editingLocalId = null
        }

        val expandedLocalId = initialExpandedLocalId ?: defaultExpandedLocalId
        items.forEach { item ->
            if (!expandedStates.containsKey(item.localId)) {
                expandedStates[item.localId] = expandedLocalId == item.localId
            }
        }
    }

    /**
     * 텍스트 필드 표시/숨김 토글
     */
    fun toggleTextField() {
        showTextField = !showTextField
    }

    /**
     * 텍스트 필드를 닫는다.
     *
     * [AddItemTextField] 가 항목 추가·포커스 해제로 스스로 물러날 때 부른다. 토글이 아니라
     * 단방향이어야 한다 — 그 시점의 [showTextField] 는 항상 true 이므로 토글로도 닫히지만,
     * 의도가 "닫는다" 인 곳에서 토글을 쓰면 이후 호출 순서 변경에 취약해진다.
     */
    fun hideTextField() {
        showTextField = false
    }

    /**
     * 아이템 expanded 상태 토글
     */
    fun toggleItemExpanded(localId: Int) {
        expandedStates[localId] = !(expandedStates[localId] ?: false)
    }

    /**
     * 아이템 인라인 편집 모드 시작
     */
    fun startEditing(localId: Int) {
        editingLocalId = localId
    }

    /**
     * 아이템 인라인 편집 모드 종료
     */
    fun stopEditing() {
        editingLocalId = null
    }
}

/** [ProcessingMethodListState]를 composition 경계마다 한 번 기억한다. */
@Composable
fun rememberProcessingMethodListState(initialShowTextField: Boolean = false): ProcessingMethodListState =
    remember {
        ProcessingMethodListState(
            initialShowTextField = initialShowTextField,
        )
    }
