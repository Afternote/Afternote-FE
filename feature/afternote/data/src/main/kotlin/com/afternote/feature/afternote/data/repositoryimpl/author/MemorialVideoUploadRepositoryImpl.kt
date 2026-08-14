package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.domain.repository.VideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MemorialVideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.VideoUploadOutcome
import javax.inject.Inject

// DIRECTORY_AFTERNOTES 는 같은 패키지 [MemorialPhotoUploadRepositoryImpl] 의 internal const 공유.

/**
 * 추모 영상 *상태 해석* + 필요 시 업로드. 입력 [MediaInput] 의 sealed 분기로 반환:
 * - [MediaInput.Local] → [VideoUploadRepository] 위임 후 [VideoUploadOutcome.FreshlyUploaded]
 * - [MediaInput.Remote] → 입력 그대로 [VideoUploadOutcome.Existing]
 * - [MediaInput.None] → [VideoUploadOutcome.Empty]
 *
 * 로컬/원격 판별은 호출부에서 [MediaInput] 으로 끝나 있다. S3 PUT·MIME 추론·temp 파일 관리는
 * [VideoUploadRepository] 가 담당. 본 클래스는 분기 라우팅만.
 */
class MemorialVideoUploadRepositoryImpl
    @Inject
    constructor(
        private val videoUploadRepository: VideoUploadRepository,
    ) : MemorialVideoUploadRepository {
        override suspend fun resolveVideo(input: MediaInput): Result<VideoUploadOutcome> =
            when (input) {
                MediaInput.None -> {
                    Result.success(VideoUploadOutcome.Empty)
                }

                is MediaInput.Remote -> {
                    Result.success(VideoUploadOutcome.Existing(input.url))
                }

                is MediaInput.Local -> {
                    videoUploadRepository
                        .upload(input.uri, DIRECTORY_AFTERNOTES)
                        .map { VideoUploadOutcome.FreshlyUploaded(it) }
                }
            }
    }
