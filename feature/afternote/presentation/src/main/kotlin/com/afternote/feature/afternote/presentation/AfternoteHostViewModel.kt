package com.afternote.feature.afternote.presentation

import androidx.lifecycle.ViewModel
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.editor.provider.DataProviderSwitch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
         * 홈 화면 새로고침 one-shot 이벤트 스트림.
         *
         * 에디터 저장 성공·상세 삭제 등 그래프 내부에서 발생한 이벤트를 홈 화면에 전달한다.
         * Boolean flag + consume 콜백 패턴 대신 [Channel] 기반 단발성 이벤트로 모델링하여
         * "플래그를 껐다가 켰다가" 하는 보일러플레이트와 상태 동기화 버그 위험을 제거한다.
         *
         * `BUFFERED` capacity로 홈 화면이 백스택에 없는 짧은 시점에도 이벤트가 유실되지 않으며,
         * 그래프 scope 종료 시 [Channel]은 ViewModel과 함께 정리된다.
         */
        private val _homeRefreshEvents = Channel<Unit>(Channel.BUFFERED)
        val homeRefreshEvents: Flow<Unit> = _homeRefreshEvents.receiveAsFlow()

        fun requestHomeRefresh() {
            _homeRefreshEvents.trySend(Unit)
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
