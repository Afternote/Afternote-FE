package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia

/**
 * 발행 완료된 애프터노트 상세 도메인 모델.
 *
 * 모든 종류에 공통인 값만 직접 소유하고, 종류별 데이터는 [content]가 배타적으로 표현한다.
 * 임시저장 상세는 필수값 계약이 다르므로 이 모델에 nullable로 섞지 않고 별도 모델로 다룬다.
 */
data class Detail(
    val id: Long,
    val serviceName: String,
    val timestamps: DetailTimestamps,
    val receivers: List<DetailReceiver>,
    val leaveMessageBlocks: List<LeaveMessageBlock>,
    val content: DetailContent,
)

sealed interface DetailContent {
    val type: AfternoteType

    data class SocialNetwork(
        val credentials: DetailCredentials,
        val processingMethods: List<String>,
    ) : DetailContent {
        override val type: AfternoteType = AfternoteType.SOCIAL_NETWORK
    }

    data class Business(
        val credentials: DetailCredentials,
        val processingMethods: List<String>,
    ) : DetailContent {
        override val type: AfternoteType = AfternoteType.BUSINESS
    }

    data class Gallery(
        val processingMethods: List<String>,
    ) : DetailContent {
        override val type: AfternoteType = AfternoteType.GALLERY_AND_FILES
    }

    /** 추억 노트 — 곡 목록과 사진·영상. 형제 종류와 같이 종류별 데이터를 필드로 직접 든다. */
    data class Memorial(
        val songs: List<DetailSong>,
        val media: MemorialMedia,
    ) : DetailContent {
        override val type: AfternoteType = AfternoteType.MEMORIAL
    }

    data object Estate : DetailContent {
        override val type: AfternoteType = AfternoteType.ESTATE
    }
}

data class DetailTimestamps(
    val updatedAt: String,
)

data class DetailCredentials(
    val id: String,
    val password: String,
)

/**
 * 애프터노트에 지정된 수신자.
 *
 * 식별자 없는 항목은 매퍼 경계에서 걸러, 도메인부터는 [receiverId] 가 있는 수신자만 다룬다.
 */
data class DetailReceiver(
    val receiverId: Long,
    val name: String,
    val relation: String,
)
