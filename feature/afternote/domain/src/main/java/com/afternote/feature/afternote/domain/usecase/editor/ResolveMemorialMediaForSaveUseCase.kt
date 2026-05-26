package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.repository.author.MemorialPhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialVideoUploadRepository
import javax.inject.Inject

class MemorialVideoSaveException(
    cause: Throwable,
) : Exception("영상 업로드에 실패했습니다.", cause)

class MemorialPhotoSaveException(
    cause: Throwable,
) : Exception("영정 사진 업로드에 실패했습니다.", cause)

/**
 * 추모 영상·영정 사진 로컬 URI 업로드 후 저장 페이로드에 들어갈 URL 묶음을 해석한다.
 *
 * POST/PATCH 동일 규칙 — 백엔드가 `S3Service.resolvePublicUrl(key)` 로 영구 public URL 을 발급하므로
 * 클라이언트가 GET 응답에서 받은 URL 을 그대로 PATCH 페이로드에 다시 보낼 수 있다. 즉 PATCH 전용 분기
 * (`isUpdate`, presigned 마커 검사) 없이 단일 해석 결과 사용.
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
        ): Result<ResolvedMemorialMediaForSave> {
            val resolvedVideoUrl = resolveVideoUrlForSave(funeralVideoUrl).getOrElse { return Result.failure(it) }
            val resolvedMemorialPhotoUrl =
                resolveMemorialPhotoUrlForSave(
                    memorialPhotoUrl = memorialPhotoUrl,
                    pickedMemorialPhotoUri = pickedMemorialPhotoUri,
                ).getOrElse { return Result.failure(it) }
            return Result.success(
                ResolvedMemorialMediaForSave(
                    resolvedVideoUrl = resolvedVideoUrl,
                    resolvedMemorialPhotoUrl = resolvedMemorialPhotoUrl,
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

        private companion object {
            const val CONTENT_SCHEME = "content://"
        }
    }
