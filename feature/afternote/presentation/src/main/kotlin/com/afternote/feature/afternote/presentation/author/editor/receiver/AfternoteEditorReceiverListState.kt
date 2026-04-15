package com.afternote.feature.afternote.presentation.author.editor.receiver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

/**
 * AfternoteEditorReceiverList의 상태를 관리하는 State Holder
 */
@Stable
class AfternoteEditorReceiverListState(
    initialShowTextField: Boolean = false,
) {
    var showTextField by mutableStateOf(initialShowTextField)
        private set

    val expandedStates = mutableStateMapOf<String, Boolean>()

    /**
     * 초기화: 수신자들의 expanded 상태 설정
     */
    fun initializeExpandedStates(
        afternoteEditReceivers: List<AfternoteEditorReceiver>,
        initialExpandedItemId: String?,
    ) {
        afternoteEditReceivers.forEach { receiver ->
            if (!expandedStates.containsKey(receiver.id)) {
                expandedStates[receiver.id] = initialExpandedItemId == receiver.id
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
}

/**
 * AfternoteEditorReceiverListState를 생성하는 Composable 함수
 */
@Composable
fun rememberAfternoteEditorReceiverListState(initialShowTextField: Boolean = false): AfternoteEditorReceiverListState =
    remember {
        AfternoteEditorReceiverListState(
            initialShowTextField = initialShowTextField,
        )
    }
