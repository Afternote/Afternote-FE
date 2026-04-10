package com.afternote.feature.afternote.presentation

import androidx.lifecycle.ViewModel
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.MemorialPlaylistStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * [com.afternote.core.ui.Route.Afternote] 서브그래프 스코프 공유 객체.
 *
 * 작성자 목록의 SSOT는 [com.afternote.feature.afternote.domain.repository.AfternoteRepository]이며,
 * 홈↔에디터 간 **리스트 스냅샷을 여기서 전달하지 않는다** (과거 `items` 결합 제거).
 *
 * 이 VM이 들고 가는 것은 **세션 스코프 UI 초안**뿐이다: 플레이리스트 편집 버퍼, 에디터 파사드(`AfternoteEditorState`) 참조.
 * 에디터 비즈니스 폼 SSOT는 에디터 `ViewModel`의 `EditorFormState` + `SavedStateHandle` JSON 스냅샷이다.
 * 편집 본문의 진실은 에디터 쪽 [com.afternote.feature.afternote.domain.repository.AfternoteRepository.getDetail] 등 Data Layer가 담당한다.
 */
@HiltViewModel
class AfternoteHostViewModel
    @Inject
    constructor() : ViewModel() {
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
    }
