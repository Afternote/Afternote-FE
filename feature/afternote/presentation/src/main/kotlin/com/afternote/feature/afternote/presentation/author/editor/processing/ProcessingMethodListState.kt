package com.afternote.feature.afternote.presentation.author.editor.processing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem

/**
 * ProcessingMethodList의 상태를 관리하는 State Holder
 */
@Stable
class ProcessingMethodListState(
    initialShowTextField: Boolean = false,
    initialExpandedItemId: String? = null,
) {
    var showTextField by mutableStateOf(initialShowTextField)
        private set

    var editingItemId by mutableStateOf<String?>(null)
        private set

    val expandedStates = mutableStateMapOf<String, Boolean>()

    /** Stored from constructor for use when [initializeExpandedStates] is called with null. */
    private val defaultExpandedItemId: String? = initialExpandedItemId

    /**
     * 현재 [items]에 맞춰 expanded 맵을 동기화한다.
     *
     * - 목록에서 사라진 id의 expanded 항목은 제거한다.
     * - 편집 중이던 행이 목록에서 제거되면 편집 모드를 해제한다.
     * - **이미 존재하는 id**의 expanded 값은 덮어쓰지 않는다(열림/드롭다운 상태 보존).
     * - **새로 나타난 id**만 [initialExpandedItemId]·[defaultExpandedItemId] 기준으로 시드한다.
     */
    fun initializeExpandedStates(
        items: List<ProcessingMethodItem>,
        initialExpandedItemId: String?,
    ) {
        val itemIds = items.mapTo(mutableSetOf()) { it.id }
        expandedStates.keys.toList().forEach { id ->
            if (id !in itemIds) {
                expandedStates.remove(id)
            }
        }
        val editing = editingItemId
        if (editing != null && editing !in itemIds) {
            editingItemId = null
        }

        val expandedId = initialExpandedItemId ?: defaultExpandedItemId
        items.forEach { item ->
            if (!expandedStates.containsKey(item.id)) {
                expandedStates[item.id] = expandedId == item.id
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
     * 아이템 expanded 상태 토글
     */
    fun toggleItemExpanded(itemId: String) {
        expandedStates[itemId] = !(expandedStates[itemId] ?: false)
    }

    /**
     * 아이템 인라인 편집 모드 시작
     */
    fun startEditing(itemId: String) {
        editingItemId = itemId
    }

    /**
     * 아이템 인라인 편집 모드 종료
     */
    fun stopEditing() {
        editingItemId = null
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
