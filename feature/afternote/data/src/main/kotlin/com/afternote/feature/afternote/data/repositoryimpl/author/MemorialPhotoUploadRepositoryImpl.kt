package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
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
 * 영정 사진 *상태 해석* + 필요 시 업로드. 입력 [MediaInput] 의 sealed 분기:
 * - [MediaInput.Local] → [PhotoUploadRepository] 업로드 후 [PhotoUploadOutcome.FreshlyUploaded]
 * - [MediaInput.Remote] → 입력 그대로 [PhotoUploadOutcome.Existing]
 * - [MediaInput.None] → [PhotoUploadOutcome.Empty]
 *
 * 픽 우선순위·로컬/원격 판별은 호출부가 [MediaInput] 을 구성할 때 끝낸다.
 */
class MemorialPhotoUploadRepositoryImpl
    @Inject
    constructor(
        private val photoUploadRepository: PhotoUploadRepository,
    ) : MemorialPhotoUploadRepository {
        override suspend fun resolvePhoto(input: MediaInput): Result<PhotoUploadOutcome> =
            when (input) {
                MediaInput.None -> {
                    Result.success(PhotoUploadOutcome.Empty)
                }

                is MediaInput.Remote -> {
                    Result.success(PhotoUploadOutcome.Existing(input.url))
                }

                is MediaInput.Local -> {
                    photoUploadRepository
                        .upload(input.uri, DIRECTORY_AFTERNOTES)
                        .map { PhotoUploadOutcome.FreshlyUploaded(it) }
                }
            }
    }
