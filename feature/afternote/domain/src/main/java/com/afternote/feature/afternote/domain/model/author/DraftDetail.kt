package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia

/**
 * 임시저장 애프터노트 상세 — 이어쓰기(에디터 프리필) 전용 모델.
 *
 * 서버는 상세 응답을 `isDraft` 로 갈라 준다(`AfternotedetailResponse` 의 `Draft` / `Published*`).
 * 임시저장은 카테고리별 필수값 검증을 건너뛰므로(`AfternoteValidator`) 종류별 값이 **아직 안 담긴 상태**
 * 그대로 내려온다 — 그 «아직 없음» 이 이 모델의 nullable·빈 목록이다. 발행 상세([Detail])는 서버가
 * 필수값을 강제하니 같은 모델로 뭉치지 않는다.
 *
 * 종류별 값을 [DetailContent] 처럼 sealed 로 가르지 않고 평평하게 두는 이유는 소비처가 에디터 하나뿐이라서다 —
 * 에디터는 어차피 종류별 폼을 모두 들고 있고, sealed 로 가르면 관심 없는 가지를 떠안는다.
 */
data class DraftDetail(
    val id: Long,
    val type: AfternoteType,
    val serviceName: String,
    val timestamps: DetailTimestamps,
    val receivers: List<DetailReceiver>,
    val leaveMessageBlocks: List<LeaveMessageBlock>,
    /** SOCIAL·BUSINESS 의 계정 정보. 통째로 미작성이면 null, 한쪽만 채웠으면 빈 문자열로 온다. */
    val credentials: DetailCredentials?,
    val processingMethods: List<String>,
    /** MEMORIAL 의 곡 목록. 한 곡도 안 담았으면 빈 목록(서버는 `playlist` 자체를 생략한다). */
    val songs: List<DetailSong>,
    /** MEMORIAL 의 영정사진·추모 영상. 하나도 안 담았어도 빈 값으로 온다. */
    val media: MemorialMedia,
)
