package com.afternote.feature.afternote.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserProfileCacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * [com.afternote.core.ui.Route.Afternote] 서브그래프 스코프의 상태 정본.
 *
 * 에디터 flow 상태는 flow-scoped
 * [com.afternote.feature.afternote.presentation.editor.AfternoteEditorViewModel]이 담당한다.
 * 본 ViewModel은 Afternote 그래프 전체에서 공유하는 사용자 상태만 보유한다.
 */
@HiltViewModel
class AfternoteHostViewModel
    @Inject
    constructor(
        userProfileRepository: UserProfileCacheRepository,
    ) : ViewModel() {
        val isPasskeyRegistered: StateFlow<Boolean?> =
            userProfileRepository
                .isPasskeyRegisteredFlow()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )
    }
