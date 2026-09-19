package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia

/**
 * 임시저장 애프터노트 상세 — 이어쓰기(에디터 프리필)용.
 *
 * 서버는 상세 응답을 `isDraft` 로 갈라 준다(`AfternotedetailResponse` 의 `Draft` / `Published*`).
 * 임시저장은 카테고리별 필수값 검증을 건너뛰므로(`AfternoteValidator`) 종류별 값이 **아직 안 담긴 상태**
 * 그대로 내려온다 — 그 «아직 없음» 이 [DraftContent] 의 nullable·빈 목록이다. 발행 상세([Detail])는
 * 서버가 필수값을 강제하니 같은 모델로 뭉치지 않는다.
 *
 * 공통값만 직접 소유하고 종류별 값은 [content] 가 배타적으로 표현하는 것은 [Detail] 과 같다 —
 * 임시저장이라고 종류의 경계까지 무너지는 것은 아니다. ESTATE 에 계정 정보 칸이 있거나 GALLERY 에
 * 곡 목록 칸이 있으면, 서버가 그 값을 실어 보내도 아무도 모르게 버려지고 잘못된 조합이 컴파일된다.
 */
data class DraftDetail(
    val id: Long,
    val serviceName: String,
    val timestamps: DetailTimestamps,
    val receivers: List<DetailReceiver>,
    val leaveMessageBlocks: List<LeaveMessageBlock>,
    val content: DraftContent,
)

/**
 * 임시저장의 종류별 데이터. [DetailContent] 와 종류·필드 구성이 같고 **필수값 보장만 다르다** —
 * 여기서는 아직 안 쓴 칸이 null·빈 목록으로 온다.
 *
 * 발행용 [DetailContent] 를 재사용하지 않는 이유가 그것이다. 그쪽 `credentials` 는 non-null 계약이라
 * 「아직 안 씀」을 표현할 자리가 없고, 재사용하려면 그 계약을 임시저장 쪽 사정으로 느슨하게 만들어야 한다.
 */
sealed interface DraftContent {
    val type: AfternoteType

    /** 아직 안 쓴 계정 정보는 null. 한쪽만 채웠으면 빈 문자열이 섞인 값으로 온다. */
    data class SocialNetwork(
        val credentials: DetailCredentials?,
        val processingMethods: List<String>,
    ) : DraftContent {
        override val type: AfternoteType = AfternoteType.SOCIAL_NETWORK
    }

    /** 바디 스키마가 [SocialNetwork] 와 같다 — 발행 쪽과 마찬가지로 종류만 다르다. */
    data class Business(
        val credentials: DetailCredentials?,
        val processingMethods: List<String>,
    ) : DraftContent {
        override val type: AfternoteType = AfternoteType.BUSINESS
    }

    data class Gallery(
        val processingMethods: List<String>,
    ) : DraftContent {
        override val type: AfternoteType = AfternoteType.GALLERY_AND_FILES
    }

    /**
     * 추억 노트. 곡을 한 곡도 안 담았으면 서버가 `playlist` 자체를 생략하므로 [songs] 는 빈 목록이고,
     * 사진·영상도 안 담았으면 [media] 의 각 URL 이 null 이다.
     */
    data class Memorial(
        val songs: List<DetailSong>,
        val media: MemorialMedia,
    ) : DraftContent {
        override val type: AfternoteType = AfternoteType.MEMORIAL
    }

    data object Estate : DraftContent {
        override val type: AfternoteType = AfternoteType.ESTATE
    }
}
