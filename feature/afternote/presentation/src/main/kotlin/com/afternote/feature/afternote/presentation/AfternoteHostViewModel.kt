package com.afternote.feature.afternote.presentation

import androidx.lifecycle.ViewModel
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.editor.provider.DataProviderSwitch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AfternoteHostViewModel
    @Inject
    constructor(
        private val dataProviderSwitch: DataProviderSwitch,
    ) : ViewModel() {
        private val _items = MutableStateFlow<List<ListItem>>(emptyList())
        val items: StateFlow<List<ListItem>> = _items.asStateFlow()

        /**
         * 홈 화면 새로고침 요청 플래그.
         * 에디터 저장 성공·상세 삭제 등 그래프 내부에서 발생한 이벤트를
         * 홈 화면에 전달하기 위한 one-time flag. 소비 후 [consumeHomeRefresh]로 꺼진다.
         */
        private val _homeRefreshRequested = MutableStateFlow(false)
        val homeRefreshRequested: StateFlow<Boolean> = _homeRefreshRequested.asStateFlow()

        fun requestHomeRefresh() {
            _homeRefreshRequested.value = true
        }

        fun consumeHomeRefresh() {
            _homeRefreshRequested.value = false
        }

        val useFakeState: StateFlow<Boolean> = dataProviderSwitch.useFakeState

        val currentAfternoteEditorDataProvider: AfternoteEditorDataProvider
            get() = dataProviderSwitch.currentAfternoteEditorDataProvider

        /** 에디터 플로우에서 공유하는 플레이리스트 상태. Graph scope에 묶여 피처 이탈 시 자동 정리됨. */
        val playlistHolder = MemorialPlaylistStateHolder()

        /** 에디터 편집 상태. 화면 간 이동 시 유지되며, 저장/취소 시 clear. */
        var editState: AfternoteEditorState? = null
            private set

        fun updateEditState(state: AfternoteEditorState?) {
            editState = state
        }

        fun clearEditState() {
            editState = null
        }

        fun updateVisibleItems(newVisibleItems: List<ListItem>) {
            _items.value = newVisibleItems
        }
    }
