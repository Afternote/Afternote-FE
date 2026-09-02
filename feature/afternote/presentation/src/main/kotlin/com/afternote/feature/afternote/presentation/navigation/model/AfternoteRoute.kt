package com.afternote.feature.afternote.presentation.navigation.model

import com.afternote.feature.afternote.domain.AfternoteType
import kotlinx.serialization.Serializable

sealed interface AfternoteRoute {
    @Serializable
    data object AfternoteHomeRoute : AfternoteRoute

    /** 임시저장 목록 (#808). 항목을 고르면 상세가 아니라 에디터로 이어쓴다. */
    @Serializable
    data object DraftListRoute : AfternoteRoute

    @Serializable
    data class DetailRoute(
        val itemId: Long,
    ) : AfternoteRoute

    /** Editor·MemorialPlaylist·AddSong이 공유하는 flow 범위와 생성/수정 인자. */
    @Serializable
    data class EditorFlowRoute(
        val itemId: Long? = null,
        val initialType: AfternoteType,
    ) : AfternoteRoute

    /** [EditorFlowRoute]의 시작 화면. flow 인자는 부모 route가 소유한다. */
    @Serializable
    data object EditorRoute : AfternoteRoute

    /** [EditorRoute] 위에 쌓이는 수신자 선택 화면 (#540). 선택 결과는 SavedStateHandle 로 반환한다. */
    @Serializable
    data object SelectReceiverRoute : AfternoteRoute

    @Serializable
    data object AddSongRoute : AfternoteRoute

    @Serializable
    data object MemorialPlaylistRoute : AfternoteRoute

    @Serializable
    data object FingerprintLoginRoute : AfternoteRoute
}
