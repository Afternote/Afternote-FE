package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail

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

    data class Memorial(
        val memorial: MemorialDetail,
    ) : DetailContent {
        override val type: AfternoteType = AfternoteType.MEMORIAL
    }

    data object Estate : DetailContent {
        override val type: AfternoteType = AfternoteType.ESTATE
    }
}

data class DetailTimestamps(
    val createdAt: String,
    val updatedAt: String,
)

data class DetailCredentials(
    val id: String,
    val password: String,
)

/**
 * 애프터노트에 지정된 수신자.
 *
 * [receiverId] 는 서버 스펙상 필수다(상세 응답의 `ReceiverRequest` 는 이 필드 하나뿐, nullable 표기 없음 —
 * 2026-08-02 Swagger 실측). DTO 는 방어적으로 nullable 이지만 그 경계는 매퍼가 흡수하고,
 * 도메인부터는 식별자가 있는 수신자만 다룬다.
 *
 * name·relation·phone 은 이 응답에 실리지 않아 현재 빈 문자열로 온다 — 서버 확장은 Afternote-BE #81.
 */
data class DetailReceiver(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val phone: String,
)
