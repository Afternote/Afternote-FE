package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailReceiverDto
import com.afternote.feature.afternote.data.dto.AfternotePlaylistDto
import com.afternote.feature.afternote.data.dto.AfternoteSongDto
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import kotlin.collections.mapNotNull

/**
 * 서버 `category` 를 해석하지 못하면 상세를 만들지 않는다.
 *
 * 단건이라 «항목 기각» 이 곧 실패다 — [Detail.type] 이 non-null 이라 모르는 종류를 담을 자리가 없고,
 * 임의의 종류로 메우면 라벨·아이콘·처리 방법이 다른 종류의 것으로 표시된다(#1048).
 * 던진 예외는 `AfternoteRepositoryImpl.safeCall` 이 잡아 `Result.failure` 로 옮긴다.
 */
fun AfternoteDetailDto.toDomain(): Detail {
    val resolvedType =
        requireNotNull(afternoteTypeFromServerCategory(category)) {
            "해석할 수 없는 애프터노트 종류다: afternoteId=$afternoteId category=$category"
        }
    return Detail(
        id = afternoteId,
        serviceName = title,
        timestamps = toTimestamps(),
        receivers = receivers.toDomain(),
        leaveMessageBlocks = leaveMessage.toLeaveMessageBlocks(),
        content = toDetailContent(resolvedType),
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

// 던지면 그 상세가 영영 안 열리므로 빠진 값은 빈 문자열로 낮춘다.
private fun AfternoteDetailDto.toPublishedCredentials() =
    DetailCredentials(
        id = credentials?.id.orEmpty(),
        password = credentials?.password.orEmpty(),
    )

// DTO 는 방어적으로 receiverId 가 nullable 이지만 서버 스펙상 필수 필드다 — 없는 항목은
// 도메인으로 올리지 않는다(식별자 없는 수신자는 저장·수정 어디에도 쓸 수 없다).
private fun List<AfternoteDetailReceiverDto>.toDomain() = mapNotNull { it.toDomain() }

private fun AfternoteDetailDto.toTimestamps(): DetailTimestamps =
    DetailTimestamps(
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
                audioUrl = memorialAudioUrl,
            ),
    )

private fun AfternoteDetailReceiverDto.toDomain(): DetailReceiver? =
    receiverId?.let { id ->
        DetailReceiver(
            receiverId = id,
            name = name ?: "",
            relation = relation ?: "",
        )
    }

private fun AfternoteSongDto.toDomain() =
    DetailSong(
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )
