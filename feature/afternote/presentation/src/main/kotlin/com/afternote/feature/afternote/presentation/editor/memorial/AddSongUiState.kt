package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * UI state for Add Song screen (search-driven list from API).
 *
 * [errorRes] 는 문자열 리소스 ID 만 담는다 — ViewModel 이 Context/Resources 를 의존하지 않으면서(#267),
 * 예외 원문·서버 응답 본문 같은 임의 문자열이 화면까지 실려 갈 경로를 타입 차원에서 없앤다 (#664).
 * Composable 측에서 `stringResource` 로 변환.
 */
data class AddSongUiState(
    val songs: List<PlaylistSongDisplay> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
)
