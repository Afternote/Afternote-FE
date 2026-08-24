package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailReceiverDto
import com.afternote.feature.afternote.data.dto.AfternotePlaylistDto
import com.afternote.feature.afternote.data.dto.AfternoteSongDto
import com.afternote.feature.afternote.data.mapper.categoryToAfternoteType
import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.afternote.data.mapper.toLeaveMessageBlocks
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia

fun AfternoteDetailDto.toDetailDomain(): Detail {
    val afternoteType = categoryToAfternoteType(category)
    return Detail(
        id = afternoteId,
        serviceName = title,
        timestamps = toTimestamps(),
        receivers = receivers.toDomain(),
        leaveMessageBlocks = leaveMessage.toLeaveMessageBlocks(),
        content = toDetailContent(afternoteType),
    )
}

private fun AfternoteDetailDto.toDetailContent(type: AfternoteType): DetailContent =
    when (type) {
        AfternoteType.SOCIAL_NETWORK -> {
            DetailContent.SocialNetwork(
                credentials = toPublishedCredentials(),
                processingMethods = processingMethods.orEmpty(),
            )
        }

        AfternoteType.BUSINESS -> {
            DetailContent.Business(
                credentials = toPublishedCredentials(),
                processingMethods = processingMethods.orEmpty(),
            )
        }

        AfternoteType.GALLERY_AND_FILES -> {
            DetailContent.Gallery(
                processingMethods = processingMethods.orEmpty(),
            )
        }

        AfternoteType.MEMORIAL -> {
            DetailContent.Memorial(
                memorial = requireNotNull(memorial) { "playlist is required for MEMORIAL detail" }.toDomain(),
            )
        }

        AfternoteType.ESTATE -> {
            DetailContent.Estate
        }
    }

// BE 상세 조립은 id·password 중 하나만 있어도 credentials 를 만들고(OR), 수정 검증은 발행 상태에서도
// 두 값을 강제하지 않는다 — 여기서 던지면 그 상세가 영영 안 열리므로 없는 값은 빈 문자열로 낮춘다.
// 두 값 모두 null 이면 credentials 객체 자체가 생략된다.
private fun AfternoteDetailDto.toPublishedCredentials() =
    DetailCredentials(
        id = credentials?.id.orEmpty(),
        password = credentials?.password.orEmpty(),
    )

// DTO 는 방어적으로 receiverId 가 nullable 이지만 서버 스펙상 필수 필드다 — 없는 항목은
// 도메인으로 올리지 않는다(식별자 없는 수신자는 저장·수정 어디에도 쓸 수 없다).
private fun List<AfternoteDetailReceiverDto>?.toDomain() = this?.mapNotNull { it.toDomain() }.orEmpty()

private fun AfternoteDetailDto.toTimestamps(): DetailTimestamps =
    DetailTimestamps(
        createdAt = formatDateFromServer(createdAt),
        updatedAt = formatDateFromServer(updatedAt),
    )

private fun AfternotePlaylistDto.toDomain() =
    MemorialDetail(
        songs = songs.map { it.toDomain() },
        media =
            MemorialMedia(
                photoUrl = memorialPhotoUrl,
                videoUrl = memorialVideo?.videoUrl,
                thumbnailUrl = memorialVideo?.thumbnailUrl,
            ),
    )

private fun AfternoteDetailReceiverDto.toDomain(): DetailReceiver? =
    receiverId?.let { id ->
        DetailReceiver(
            receiverId = id,
            name = name ?: "",
            relation = relation ?: "",
            phone = phone ?: "",
        )
    }

private fun AfternoteSongDto.toDomain() =
    DetailSong(
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )
