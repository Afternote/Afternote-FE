package com.afternote.feature.afternote.presentation

import androidx.lifecycle.ViewModel
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.editor.provider.DataProviderSwitch
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteEditorStateHandling
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

        val useFakeState: StateFlow<Boolean> = dataProviderSwitch.useFakeState

        val currentAfternoteEditorDataProvider: AfternoteEditorDataProvider
            get() = dataProviderSwitch.currentAfternoteEditorDataProvider

        /** 에디터 플로우에서 공유하는 플레이리스트 상태. Graph scope에 묶여 피처 이탈 시 자동 정리됨. */
        val playlistHolder = MemorialPlaylistStateHolder()

        /** 에디터 편집 상태. 화면 간 이동 시 유지되며, 저장/취소 시 clear. */
        private var editState: AfternoteEditorState? = null

        val editHandling: AfternoteEditorStateHandling
            get() =
                AfternoteEditorStateHandling(
                    state = editState,
                    onStateChanged = { editState = it },
                    onClear = { editState = null },
                )

        fun updateVisibleItems(newVisibleItems: List<ListItem>) {
            _items.value = newVisibleItems
        }
    }
