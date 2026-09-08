package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.VideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialAudioUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import javax.inject.Inject

/**
 * 서버 S3 의 애프터노트 미디어 폴더명. presigned URL 경로에 박혀 `bucket/afternotes/<file>` 형태가 됨. 서버와 약속된 문자열.
 *
 * 같은 패키지의 [MemorialThumbnailUploadRepositoryImpl] · [MemorialAudioUploadRepositoryImpl] 가
 * 이 값을 공유하므로 `internal`.
 */
internal const val DIRECTORY_AFTERNOTES = "afternotes"

/**
 * 추억 노트 미디어 *상태 해석* + 필요 시 업로드. 업로드 자체(presigned 발급·S3 PUT·MIME 추론)는
 * [PhotoUploadRepository] · [VideoUploadRepository] · [MemorialAudioUploadRepository] 가 담당하고,
 * 본 클래스는 분기 라우팅만 한다.
 */
class MemorialMediaUploadRepositoryImpl
    @Inject
    constructor(
        private val photoUploadRepository: PhotoUploadRepository,
        private val videoUploadRepository: VideoUploadRepository,
        private val memorialAudioUploadRepository: MemorialAudioUploadRepository,
    ) : MemorialMediaUploadRepository {
        override suspend fun resolve(
            input: MediaInput,
            kind: MediaKind,
        ): Result<String?> =
            when (input) {
                MediaInput.None -> {
                    Result.success(null)
                }

                is MediaInput.Remote -> {
                    Result.success(input.url)
                }

                is MediaInput.Local -> {
                    when (kind) {
                        MediaKind.PHOTO -> photoUploadRepository.upload(input.uri, DIRECTORY_AFTERNOTES).map { it.fileUrl }

                        MediaKind.VIDEO -> videoUploadRepository.upload(input.uri, DIRECTORY_AFTERNOTES).map { it.fileUrl }

                        // 음성은 디렉터리를 인자로 받지 않는다 — 추억 노트 전용 계약이라 구현이 직접 안다.
                        MediaKind.AUDIO -> memorialAudioUploadRepository.upload(input.uri)
                    }
                }
            }
    }
