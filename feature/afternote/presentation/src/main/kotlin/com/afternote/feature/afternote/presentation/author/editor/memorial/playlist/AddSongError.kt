package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

/**
 * `AddSong` 검색 흐름의 UI 에러 표현.
 *
 * ViewModel 은 "어떤 에러" 만 분기하고, string resolve 는 UI 레이어가 담당한다 — VM 에서 Android
 * Framework (`Context`/`Resources`) 의존을 제거하기 위한 sealed 구조. 자세한 근거는 이슈 #267.
 */
sealed interface AddSongError {
    /** Repository 실패가 메시지 없이 떨어진 일반 케이스. UI 가 일괄 메시지로 폴백. */
    data object SearchFailedGeneric : AddSongError

    /** Repository 실패가 사용자 노출 가능 메시지를 동반한 케이스. */
    data class SearchFailedWithMessage(
        val message: String,
    ) : AddSongError
}
