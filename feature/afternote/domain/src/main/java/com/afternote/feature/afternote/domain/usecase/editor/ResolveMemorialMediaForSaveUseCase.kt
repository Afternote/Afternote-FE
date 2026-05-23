package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.repository.author.MemorialPhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialVideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.PhotoUploadOutcome
import com.afternote.feature.afternote.domain.repository.author.VideoUploadOutcome
import javax.inject.Inject

class MemorialVideoSaveException(
    cause: Throwable,
) : Exception("영상 업로드에 실패했습니다.", cause)

class MemorialPhotoSaveException(
    cause: Throwable,
) : Exception("영정 사진 업로드에 실패했습니다.", cause)

/**
 * 추모 영상·영정 사진의 *서버 저장 직전 정리* 도메인 로직.
 *
 * Repository 가 입력 String 의 형식을 판별해 sealed [VideoUploadOutcome]/[PhotoUploadOutcome] 로
 * 결과를 주고, 본 UseCase 는 그 sealed 분기를 *생성(POST) vs 수정(PATCH)* 페이로드 규칙에 매핑한다.
 * 도메인 본문이 `"content://"`, `"X-Amz-"` 같은 인프라 형식 디테일 문자열을 직접 비교하지 않는다.
 *
 * **PATCH 페이로드 규칙**:
 * - [VideoUploadOutcome.Existing] → 페이로드에서 *제거* (`null`).
 *   영상 URL 은 presigned 일 가능성이 있어 그대로 보내면 만료 후 깨지거나 서버가 *새 자원* 으로 오해.
 *   사진은 백엔드 `fileUrl` 이 영구 URL 이라 Existing 도 동일 URL 재전송 — POST 와 같은 값.
 * - [VideoUploadOutcome.FreshlyUploaded] / [PhotoUploadOutcome.FreshlyUploaded] → URL 그대로 전송.
 *   서버가 *새 자원* 으로 등록.
 * - [VideoUploadOutcome.Empty] / [PhotoUploadOutcome.Empty] → null.
 *
 * 반환 [ResolvedMemorialMediaForSave] 의 4개 필드는 *생성(POST)* 과 *수정(PATCH)* 에서 각각 다른
 * 필드를 사용 — 자세한 사용 규칙은 그 데이터 클래스 KDoc 참고.
 */
class ResolveMemorialMediaForSaveUseCase
    @Inject
    constructor(
        private val memorialVideoUploadRepository: MemorialVideoUploadRepository,
        private val memorialPhotoUploadRepository: MemorialPhotoUploadRepository,
    ) {
        /**
         * @param funeralVideoUrl 현재 ViewModel 의 영상 URL — 로컬/원격 중 하나, 또는 null.
         * @param memorialPhotoUrl 현재 영정 사진의 *영구 원격* URL (없으면 null).
         * @param pickedMemorialPhotoUri 사용자가 *방금 새로 고른* 영정 사진의 로컬 URI (안 골랐으면 null).
         * @param funeralThumbnailUrl 영상에 딸린 썸네일 URL — 수정(PATCH) 시 영상 페이로드와 짝으로 처리.
         * @param isUpdate true 면 PATCH (Existing 페이로드 제거 규칙 적용), false 면 POST.
         */
        suspend operator fun invoke(
            funeralVideoUrl: String?,
            memorialPhotoUrl: String?,
            pickedMemorialPhotoUri: String?,
            funeralThumbnailUrl: String?,
            isUpdate: Boolean,
        ): Result<ResolvedMemorialMediaForSave> {
            // 두 Repository 중 하나라도 실패 → 도메인 예외로 wrap 후 invoke 자체를 즉시 종료
            // (getOrElse 람다 안의 `return` 은 non-local return — 함수 전체에서 빠져나감)
            val videoOutcome =
                memorialVideoUploadRepository
                    .resolveVideo(funeralVideoUrl)
                    .getOrElse { return Result.failure(MemorialVideoSaveException(it)) }

            val photoOutcome =
                memorialPhotoUploadRepository
                    .resolvePhoto(existingUrl = memorialPhotoUrl, pickedUri = pickedMemorialPhotoUri)
                    .getOrElse { return Result.failure(MemorialPhotoSaveException(it)) }

            val resolvedVideoUrl = videoOutcome.urlOrNull()
            val resolvedMemorialPhotoUrl = photoOutcome.urlOrNull()
            val videoUrlForUpdate = if (isUpdate) videoOutcome.urlForUpdate() else resolvedVideoUrl
            val thumbnailForUpdate = if (videoUrlForUpdate == null) null else funeralThumbnailUrl

            return Result.success(
                ResolvedMemorialMediaForSave(
                    resolvedVideoUrl = resolvedVideoUrl,
                    resolvedMemorialPhotoUrl = resolvedMemorialPhotoUrl,
                    videoUrlForUpdate = videoUrlForUpdate,
                    funeralThumbnailUrlForUpdate = thumbnailForUpdate,
                ),
            )
        }

        private fun VideoUploadOutcome.urlOrNull(): String? =
            when (this) {
                VideoUploadOutcome.Empty -> null
                is VideoUploadOutcome.Existing -> url
                is VideoUploadOutcome.FreshlyUploaded -> url
            }

        private fun PhotoUploadOutcome.urlOrNull(): String? =
            when (this) {
                PhotoUploadOutcome.Empty -> null
                is PhotoUploadOutcome.Existing -> url
                is PhotoUploadOutcome.FreshlyUploaded -> url
            }

        /**
         * PATCH 페이로드에 들어갈 영상 URL — [VideoUploadOutcome.Existing] 만 *변경 없음* 신호로 null.
         */
        private fun VideoUploadOutcome.urlForUpdate(): String? =
            when (this) {
                VideoUploadOutcome.Empty -> null
                is VideoUploadOutcome.Existing -> null
                is VideoUploadOutcome.FreshlyUploaded -> url
            }
    }
