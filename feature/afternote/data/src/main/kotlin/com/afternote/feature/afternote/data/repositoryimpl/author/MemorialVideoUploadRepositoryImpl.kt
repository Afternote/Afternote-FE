package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.domain.repository.VideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialVideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.VideoUploadOutcome
import javax.inject.Inject

// DIRECTORY_AFTERNOTES, LOCAL_CONTENT_SCHEME 는 같은 패키지 [MemorialPhotoUploadRepositoryImpl] 의 internal const 공유.

/**
 * 추모 영상 *상태 해석* + 필요 시 업로드.
 *
 * 입력 String 의 형식을 판별해 sealed 분기로 반환:
 * - 로컬 `content://` URI → [VideoUploadRepository] 위임 후 [VideoUploadOutcome.FreshlyUploaded]
 * - 원격 HTTPS URL → 입력 그대로 [VideoUploadOutcome.Existing]
 * - null/blank → [VideoUploadOutcome.Empty]
 *
 * `content://` prefix 비교는 *data 레이어 안* 에 격리 — 도메인은 인프라 형식 디테일을 모름.
 * S3 PUT·MIME 추론·temp 파일 관리는 [VideoUploadRepository] 가 담당. 본 클래스는 분기 라우팅만.
 */
class MemorialVideoUploadRepositoryImpl
    @Inject
    constructor(
        private val videoUploadRepository: VideoUploadRepository,
    ) : MemorialVideoUploadRepository {
        override suspend fun resolveVideo(input: String?): Result<VideoUploadOutcome> {
            if (input.isNullOrBlank()) return Result.success(VideoUploadOutcome.Empty)
            if (!input.startsWith(LOCAL_CONTENT_SCHEME)) {
                return Result.success(VideoUploadOutcome.Existing(input))
            }
            return videoUploadRepository
                .upload(input, DIRECTORY_AFTERNOTES)
                .map { VideoUploadOutcome.FreshlyUploaded(it) }
        }
    }
