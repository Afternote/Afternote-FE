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
import com.afternote.feature.afternote.domain.model.author.DraftDetail
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import kotlin.collections.mapNotNull

/**
 * **발행 완료 상세** 전용 변환. 임시저장은 [toDraftDomain] 으로 간다.
 *
 * 서버는 상세 응답을 `isDraft` 로 갈라 준다(`AfternotedetailResponse` 의 `oneOf`: `Draft` /
 * `Published` / `PublishedPlaylist`). 발행 완료는 서버가 응답을 조립하면서 카테고리별 필수값을
 * 강제하므로(`requirePublishedPlaylist` · `requirePublishedCredentials`) 여기서는 그 보장을 그대로
 * 타입으로 받는다 — 빠져 있으면 계약 위반이라 낮추지 않고 실패로 옮긴다.
 *
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

/**
 * **임시저장 상세** 변환 — 이어쓰기(에디터 프리필)용.
 *
 * 임시저장은 카테고리별 필수값 검증을 건너뛰므로(`AfternoteValidator`) 종류별 값이 통째로 빠질 수 있다 —
 * 곡을 한 곡도 안 담은 PLAYLIST 는 `playlist` 자체가 오지 않고, 계정 정보를 아직 안 쓴 SOCIAL 은
 * `credentials` 가 없다. 그 «아직 없음» 은 계약 위반이 아니라 임시저장의 정상 상태라 던지지 않는다.
 *
 * 종류만은 발행분과 같은 이유로 엄격하다 — 해석 못 하는 `category` 는 폼을 못 고른다(#1048).
 */
fun AfternoteDetailDto.toDraftDomain(): DraftDetail {
    val resolvedType =
        requireNotNull(afternoteTypeFromServerCategory(category)) {
            "해석할 수 없는 애프터노트 종류다: afternoteId=$afternoteId category=$category"
        }
    return DraftDetail(
        id = afternoteId,
        type = resolvedType,
        serviceName = title,
        timestamps = toTimestamps(),
        receivers = receivers.toDomain(),
        leaveMessageBlocks = leaveMessage.toLeaveMessageBlocks(),
        credentials = toDraftCredentials(),
        processingMethods = processingMethods.orEmpty(),
        songs = memorial?.songs?.map { it.toDomain() }.orEmpty(),
        media =
            MemorialMedia(
                photoUrl = memorial?.memorialPhotoUrl,
                videoUrl = memorial?.memorialVideo?.videoUrl,
                thumbnailUrl = memorial?.memorialVideo?.thumbnailUrl,
            ),
    )
}

// 한쪽만 채운 임시저장은 그 한쪽만 살린다 — 통째로 미작성이면 null 로 남겨 «아직 안 씀» 을 그대로 전한다.
private fun AfternoteDetailDto.toDraftCredentials(): DetailCredentials? =
    credentials?.let {
        DetailCredentials(
            id = it.id.orEmpty(),
            password = it.password.orEmpty(),
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
            requireNotNull(memorial) {
                // 발행 PLAYLIST 는 서버가 최소 1곡을 강제한다 — 여기 오면 임시저장이 발행 경로로 잘못 들어온 것이다.
                "발행 상세에 playlist 가 없다: afternoteId=$afternoteId"
            }.toMemorialContent()
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
