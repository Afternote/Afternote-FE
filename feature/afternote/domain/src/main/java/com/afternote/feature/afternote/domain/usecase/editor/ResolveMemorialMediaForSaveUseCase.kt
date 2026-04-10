package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.repository.MemorialPhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.MemorialVideoUploadRepository
import javax.inject.Inject

class MemorialVideoSaveException(
    cause: Throwable,
) : Exception("영상 업로드에 실패했습니다.", cause)

class MemorialPhotoSaveException(
    cause: Throwable,
) : Exception("영정 사진 업로드에 실패했습니다.", cause)

/**
 * 추모 영상·영정 사진 로컬 URI 업로드 및 PATCH 시 presigned URL 생략 규칙 적용.
 */
class ResolveMemorialMediaForSaveUseCase
    @Inject
    constructor(
        private val memorialVideoUploadRepository: MemorialVideoUploadRepository,
        private val memorialPhotoUploadRepository: MemorialPhotoUploadRepository,
    ) {
        suspend operator fun invoke(
            funeralVideoUrl: String?,
            memorialPhotoUrl: String?,
            pickedMemorialPhotoUri: String?,
            funeralThumbnailUrl: String?,
            isUpdate: Boolean,
        ): Result<ResolvedMemorialMediaForSave> {
            val resolvedVideoUrl = resolveVideoUrlForSave(funeralVideoUrl).getOrElse { return Result.failure(it) }
            val resolvedMemorialPhotoUrl =
                resolveMemorialPhotoUrlForSave(
                    memorialPhotoUrl = memorialPhotoUrl,
                    pickedMemorialPhotoUri = pickedMemorialPhotoUri,
                ).getOrElse { return Result.failure(it) }
            val videoUrlForUpdate = videoUrlForUpdateRequest(isUpdate, resolvedVideoUrl)
            val thumbnailForUpdate =
                if (videoUrlForUpdate == null) null else funeralThumbnailUrl
            return Result.success(
                ResolvedMemorialMediaForSave(
                    resolvedVideoUrl = resolvedVideoUrl,
                    resolvedMemorialPhotoUrl = resolvedMemorialPhotoUrl,
                    videoUrlForUpdate = videoUrlForUpdate,
                    funeralThumbnailUrlForUpdate = thumbnailForUpdate,
                ),
            )
        }

        private suspend fun resolveVideoUrlForSave(funeralVideoUrl: String?): Result<String?> {
            if (funeralVideoUrl.isNullOrBlank()) return Result.success(null)
            if (!funeralVideoUrl.startsWith(CONTENT_SCHEME)) return Result.success(funeralVideoUrl)
            return memorialVideoUploadRepository.uploadVideo(funeralVideoUrl).fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.failure(MemorialVideoSaveException(it)) },
            )
        }

        private suspend fun resolveMemorialPhotoUrlForSave(
            memorialPhotoUrl: String?,
            pickedMemorialPhotoUri: String?,
        ): Result<String?> {
            if (!pickedMemorialPhotoUri.isNullOrBlank() &&
                pickedMemorialPhotoUri.startsWith(CONTENT_SCHEME)
            ) {
                return memorialPhotoUploadRepository.upload(pickedMemorialPhotoUri).fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(MemorialPhotoSaveException(it)) },
                )
            }
            return Result.success(memorialPhotoUrl?.takeIf { it.isNotBlank() })
        }

        private fun videoUrlForUpdateRequest(
            isUpdate: Boolean,
            resolvedVideoUrl: String?,
        ): String? {
            if (!isUpdate || resolvedVideoUrl == null) return resolvedVideoUrl
            if (resolvedVideoUrl.contains(PRESIGNED_URL_MARKER)) return null
            return resolvedVideoUrl
        }

        private companion object {
            const val PRESIGNED_URL_MARKER = "X-Amz-"
            const val CONTENT_SCHEME = "content://"
        }
    }
