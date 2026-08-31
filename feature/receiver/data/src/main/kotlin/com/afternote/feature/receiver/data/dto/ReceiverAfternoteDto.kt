package com.afternote.feature.receiver.data.dto

import com.afternote.feature.afternote.data.dto.LeaveMessageBlockDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [decodingRejectedItemCount] 는 역직렬화가 실제로 센 값이어야 하므로 바깥에서 심을 수 없어야 한다.
 *
 * 이 제약은 기본값 인자로는 만들 수 없다. 주 생성자를 internal 로 두면 외부 모듈의 생성 경로가 통째로
 * 사라지고, public 으로 두면 프로퍼티가 internal 이어도 외부에서 세 번째 인자를 그대로 넘길 수 있다.
 * 그래서 3인자 주 생성자를 internal 로 잠그고 2인자 보조 생성자만 공개한다.
 *
 * `@ConsistentCopyVisibility` 는 같은 목적의 나머지 절반이다. data class 가 자동 생성하는 copy() 는
 * 주 생성자가 비공개여도 public 으로 새어 나가므로, 이 어노테이션으로 copy() 를 주 생성자와 같은
 * 가시성으로 맞춰 뒷문을 닫는다.
 */
@ConsistentCopyVisibility
@Serializable(with = ReceivedAfternoteListDtoSerializer::class)
data class ReceivedAfternoteListDto internal constructor(
    @SerialName("afternotes") val afternotes: List<ReceivedAfternoteDto>,
    @SerialName("totalCount") val totalCount: Int,
    internal val decodingRejectedItemCount: Int,
) {
    // 제외 개수를 알 필요가 없는 일반 생성 경로. 역직렬화 밖에서 만드는 DTO 는 제외가 0 건이다.
    constructor(
        afternotes: List<ReceivedAfternoteDto>,
        totalCount: Int,
    ) : this(
        afternotes = afternotes,
        totalCount = totalCount,
        decodingRejectedItemCount = 0,
    )
}

@Serializable
data class ReceivedAfternoteDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("category") val category: String,
    @SerialName("leaveMessage") val leaveMessage: List<LeaveMessageBlockDto>? = null,
    @SerialName("senderId") val senderId: Long? = null,
    @SerialName("senderName") val senderName: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class ReceivedAfternoteDetailDto(
    @SerialName("id") val id: Long,
    @SerialName("category") val category: String? = null,
    @SerialName("title") val serviceName: String,
    @SerialName("actions") val processingMethods: List<String>?,
    @SerialName("leaveMessage") val leaveMessage: List<LeaveMessageBlockDto>? = null,
    @SerialName("senderName") val senderName: String,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("credentials") val credentials: ReceivedCredentialsDto? = null,
    @SerialName("playlist") val playlist: ReceivedPlaylistDto? = null,
)

@Serializable
data class ReceivedCredentialsDto(
    @SerialName("id") val id: String? = null,
    @SerialName("password") val password: String? = null,
)

@Serializable
data class ReceivedPlaylistDto(
    @SerialName("atmosphere") val atmosphere: String? = null,
    @SerialName("songs") val songs: List<ReceivedSongDto>,
    @SerialName("memorialVideo") val memorialVideo: ReceivedMemorialVideoDto? = null,
)

@Serializable
data class ReceivedSongDto(
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("coverUrl") val coverUrl: String? = null,
)

@Serializable
data class ReceivedMemorialVideoDto(
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
)
