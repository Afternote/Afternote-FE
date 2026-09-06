package com.afternote.feature.afternote.presentation.navigation.model

import com.afternote.feature.afternote.domain.AfternoteType
import kotlinx.serialization.Serializable

sealed interface AfternoteRoute {
    @Serializable
    data object AfternoteHomeRoute : AfternoteRoute

    @Serializable
    data class DetailRoute(
        val itemId: Long,
    ) : AfternoteRoute

    /** Editor·MemorialPlaylist·AddSong이 공유하는 flow 범위와 생성/수정 인자. */
    @Serializable
    data class EditorFlowRoute(
        val itemId: Long? = null,
        val initialType: AfternoteType,
        /**
         * 이어쓰기로 들어왔는지 (#808). 서버 상세는 하나인데 응답 형태가 갈리므로
         * (`AfternotedetailResponse` 의 Draft / Published*) 무엇으로 읽을지를 여는 쪽이 정한다.
         */
        val isDraft: Boolean = false,
    ) : AfternoteRoute

    /** [EditorFlowRoute]의 시작 화면. flow 인자는 부모 route가 소유한다. */
    @Serializable
    data object EditorRoute : AfternoteRoute

    /** [EditorRoute] 위에 쌓이는 수신자 선택 화면 (#540). 선택 결과는 SavedStateHandle 로 반환한다 (복수, #1426). */
    @Serializable
    data object SelectReceiverRoute : AfternoteRoute

    @Serializable
    data object AddSongRoute : AfternoteRoute

    @Serializable
    data object MemorialPlaylistRoute : AfternoteRoute

    @Serializable
    data object FingerprintLoginRoute : AfternoteRoute
}
