package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialPhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.PhotoUploadOutcome
import javax.inject.Inject

/**
 * 서버 S3 의 애프터노트 미디어 폴더명. presigned URL 경로에 박혀 `bucket/afternotes/<file>` 형태가 됨. 서버와 약속된 문자열.
 *
 * 같은 패키지의 [MemorialVideoUploadRepositoryImpl] · [MemorialThumbnailUploadRepositoryImpl] 가 이 값을 공유하므로 `internal`.
 */
internal const val DIRECTORY_AFTERNOTES = "afternotes"

/**
 * Android 의 *로컬 파일 URI* 스킴. 갤러리/카메라 picker 결과는 `content://...` 로 시작 → "아직 서버에 없음, 업로드 필요" 의 신호. `https://...` 같은 원격 URL 과 구분하는 용도.
 *
 * 같은 패키지의 [MemorialVideoUploadRepositoryImpl] 가 이 값을 공유하므로 `internal`.
 */
internal const val LOCAL_CONTENT_SCHEME = "content://"

/**
 * 영정 사진 *상태 해석* + 필요 시 업로드.
 *
 * `pickedUri` 가 로컬 `content://` 면 [PhotoUploadRepository] 로 업로드 후
 * [PhotoUploadOutcome.FreshlyUploaded]. 그 외엔 `existingUrl` 기준으로
 * [PhotoUploadOutcome.Existing] / [PhotoUploadOutcome.Empty] 분기.
 *
 * `content://` prefix 비교는 *data 레이어 안* 에 격리 — 도메인은 인프라 형식 디테일을 모름.
 */
class MemorialPhotoUploadRepositoryImpl
    @Inject
    constructor(
        private val photoUploadRepository: PhotoUploadRepository,
    ) : MemorialPhotoUploadRepository {
        override suspend fun resolvePhoto(
            existingUrl: String?,
            pickedUri: String?,
        ): Result<PhotoUploadOutcome> {
            if (!pickedUri.isNullOrBlank() && pickedUri.startsWith(LOCAL_CONTENT_SCHEME)) {
                return photoUploadRepository
                    .upload(pickedUri, DIRECTORY_AFTERNOTES)
                    .map { PhotoUploadOutcome.FreshlyUploaded(it) }
            }
            val fallback = existingUrl?.takeIf { it.isNotBlank() }
            return Result.success(
                if (fallback == null) PhotoUploadOutcome.Empty else PhotoUploadOutcome.Existing(fallback),
            )
        }
    }
