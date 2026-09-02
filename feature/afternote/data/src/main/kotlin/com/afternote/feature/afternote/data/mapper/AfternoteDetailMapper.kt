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
            memorial?.toMemorialContent() ?: EMPTY_MEMORIAL_CONTENT
        }

        AfternoteType.ESTATE -> {
            DetailContent.Estate
        }
    }

/**
 * 곡도 미디어도 아직 안 담은 추억 노트.
 *
 * 서버 상세는 임시저장을 걸러 내지 않고(`AfternoteService.getDetailAfternote` 는 소유자만 본다) 응답 형태로만
 * 가른다(`AfternotedetailResponse.of` 의 `Draft` 분기). 임시저장(`isDraft=true`)은 카테고리별 필수값 검증을
 * 건너뛰므로(`AfternoteValidator`) 곡을 한 곡도 안 담은 PLAYLIST 는 `playlist` 자체가 안 온다 — #808 이
 * `draftOnly=true` 목록과 이어쓰기를 붙이면 그런 임시저장이 이 분기로 들어오고, 여기서 던지면 이어쓰기 이전에
 * 조회부터 실패한다. 발행 상세는 서버가 응답을 조립하면서 최소 1곡을 강제하므로
 * (`AfternotedetailResponse.requirePublishedPlaylist`) 이 폴백이 발행분을 빈 추모 노트로 위장할 일은 없다 —
 * 계정 정보와 같은 이유로, 빠진 값은 던지지 않고 빈 값으로 낮춘다.
 */
private val EMPTY_MEMORIAL_CONTENT =
    DetailContent.Memorial(
        songs = emptyList(),
        media = MemorialMedia(photoUrl = null, videoUrl = null, thumbnailUrl = null),
    )

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

private fun AfternotePlaylistDto.toMemorialContent() =
    DetailContent.Memorial(
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
        )
    }

private fun AfternoteSongDto.toDomain() =
    DetailSong(
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )
