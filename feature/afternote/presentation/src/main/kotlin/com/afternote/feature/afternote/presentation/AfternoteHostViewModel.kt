package com.afternote.feature.afternote.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserProfileRepository
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * [com.afternote.core.ui.Route.Afternote] 서브그래프 스코프 SSOT.
 *
 * 추모(MEMORIAL) 플레이리스트 곡 목록은 Editor / MemorialPlaylist / AddSong 세 화면이 공유하므로
 * 그래프 스코프 ViewModel이 보관한다. **Compose UI 객체(TextFieldState·SnapshotStateList·UI 파사드)는 보유하지 않는다.**
 * UI 레이어는 [playlistSongs]를 [androidx.lifecycle.compose.collectAsStateWithLifecycle]로 수집하고,
 * 변경은 본 VM의 인텐트([replaceSongs]·[addSongs]·[removeSongs]·[clearAllSongs])로만 수행한다.
 *
 * 프로세스 종료 후 복원은 에디터 ViewModel의 SavedStateHandle 폼 스냅샷이 담당한다.
 * 에디터가 [com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorEvent.PrefillLoaded]를 받으면
 * 네비게이션 레이어가 [replaceSongs]로 host SSOT를 동기화한다.
 *
 * 작성자 목록 SSOT는 [com.afternote.feature.afternote.domain.repository.author.AfternoteRepository]이며,
 * 편집 본문의 진실은 에디터 [com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel]
 * 의 폼 + SavedState가 담당한다 (UI 파사드를 host VM에 캐싱하지 않는다).
 */
@HiltViewModel
class AfternoteHostViewModel
    @Inject
    constructor(
        userProfileRepository: UserProfileRepository,
    ) : ViewModel() {
        val isPasskeyRegistered: StateFlow<Boolean?> =
            userProfileRepository
                .isPasskeyRegisteredFlow()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )

        private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
        val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

        fun replaceSongs(songs: List<Song>) {
            _playlistSongs.value = songs
        }

        fun addSongs(songs: List<Song>) {
            if (songs.isEmpty()) return
            _playlistSongs.update { prev -> prev + songs }
        }

        fun removeSongs(ids: Set<String>) {
            if (ids.isEmpty()) return
            _playlistSongs.update { prev -> prev.filterNot { it.id in ids } }
        }

        fun clearAllSongs() {
            _playlistSongs.value = emptyList()
        }
    }
