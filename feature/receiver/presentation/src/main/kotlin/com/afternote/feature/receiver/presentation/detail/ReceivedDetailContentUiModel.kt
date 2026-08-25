package com.afternote.feature.receiver.presentation.detail

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.MessageBlockUiModel

/**
 * 카테고리별 수신 상세 UI 모델.
 *
 * 발신자([com.afternote.feature.afternote.presentation.author.detail.DetailContentUiModel])와 달리
 * 수신자 카드·작성자 표시명을 보유하지 않는다(받은 본인이 수신자이므로).
 */
sealed interface ReceivedDetailContentUiModel {
    data class SocialNetwork(
        val content: ReceivedSocialNetworkDetailContent,
    ) : ReceivedDetailContentUiModel

    data class Gallery(
        val content: ReceivedGalleryDetailContent,
    ) : ReceivedDetailContentUiModel

    /** 추억 카테고리 수신 상세 — [MemorialReceivedDetailScreen] 으로 표시 (#274). */
    data class Memorial(
        val content: ReceivedMemorialDetailContent,
    ) : ReceivedDetailContentUiModel

    /** BUSINESS·ESTATE 등 디자인 확정 전 placeholder. */
    data object Unimplemented : ReceivedDetailContentUiModel
}

@Immutable
data class ReceivedSocialNetworkDetailContent(
    val serviceName: String = "",
    val accountId: String = "",
    val password: String = "",
    val processingMethods: List<String> = emptyList(),
    val messageBlocks: List<MessageBlockUiModel> = emptyList(),
    val finalWriteDate: String = "",
)

@Immutable
data class ReceivedGalleryDetailContent(
    val serviceName: String = "",
    val finalWriteDate: String = "",
    val processingMethods: List<String> = emptyList(),
    val messageBlocks: List<MessageBlockUiModel> = emptyList(),
)

/**
 * 추억 노트(MEMORIAL) 카테고리 수신 상세 표시 모델.
 *
 * [MemorialReceivedDetailScreen] prototype 시그니처에 맞춘 값만 보유한다. 유언 등 서버 DTO 에
 * 대응 필드가 없는 항목은 prototype 내부 표현을 따른다.
 *
 * @property albumCovers 플레이리스트 카드에 가로 스크롤로 표시할 커버 목록(표시 데이터).
 * @property songCount "현재 N개의 노래가 담겨 있습니다" 안내용 전체 곡 수(메타데이터). `albumCovers.size` 와
 *   **독립** — [com.afternote.feature.afternote.presentation.shared.detail.song.MemorialPlaylist] 가 둘을 별도
 *   파라미터로 받아, 커버를 일부만 넘겨도 전체 개수를 정확히 표시한다. 현재 mapper 는 전체 곡을 커버로 넘겨 값이 같다.
 */
@Immutable
data class ReceivedMemorialDetailContent(
    val senderName: String = "",
    val messageBlocks: List<MessageBlockUiModel> = emptyList(),
    val albumCovers: List<AlbumCover> = emptyList(),
    val songCount: Int = 0,
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
)
